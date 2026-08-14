package org.zstack.ipsec;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmCommands;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.NormalIpRangeVO;
import org.zstack.header.network.l3.NormalIpRangeVO_;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.ipsec.vyos.VyosIPsecBackend;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.vip.VipConfigProxy;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.RangeSet;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/11/3.
 */
public class IPsecManagerImpl extends AbstractService implements IPsecManager, VipGetUsedPortRangeExtensionPoint, GlobalApiMessageInterceptor,
                     VipGetServiceReferencePoint, VirtualRouterBeforeDetachNicExtensionPoint {

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private VipConfigProxy vipProxy;
    @Autowired
    protected TagManager tagMgr;

    private final Map<String, IPsecBackend> backends = new HashMap<>();

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void populateExtensions() {
        for (IPsecBackend bkd : pluginRgty.getExtensionList(IPsecBackend.class)) {
            IPsecBackend old = backends.get(bkd.getNetworkServiceProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate IPsecBackend[%s, %s] for the network service provider" +
                        " type[%s]", old.getClass(), bkd.getClass(), bkd.getNetworkServiceProviderType()));
            }

            backends.put(bkd.getNetworkServiceProviderType(), bkd);
        }
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof IPsecConnectionMessage) {
            passThrough((IPsecConnectionMessage)msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APICreateIPsecConnectionMsg) {
            handle((APICreateIPsecConnectionMsg) msg);
        } else if (msg instanceof APIGetCandidateL3NetworksForIpSecConnectionMsg) {
            handle((APIGetCandidateL3NetworksForIpSecConnectionMsg) msg);
        } else if (msg instanceof APIGetVpcIPsecLogMsg) {
            handle((APIGetVpcIPsecLogMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(IPsecConnectionMessage msg) {
        IPsecConnectionVO vo = Q.New(IPsecConnectionVO.class).eq(IPsecConnectionVO_.uuid, msg.getIPsecConnectionUuid()).find();
        if (vo == null) {
            throw new OperationFailureException(argerr("cannot find the IPsecconnection[uuid:%s], it may have been deleted", msg.getIPsecConnectionUuid()));
        }

        IPsecconnectionBase ipsec = new IPsecconnectionBase(vo);
        ipsec.handleMessage((Message) msg);
    }

    public IPsecBackend getBackend(String l3Uuid) {
        return getBackend(l3Uuid, true);
    }

    private IPsecBackend getBackend(String l3Uuid, boolean errOnException) {
        SimpleQuery<NetworkServiceL3NetworkRefVO> rq = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
        rq.select(NetworkServiceL3NetworkRefVO_.networkServiceProviderUuid);
        rq.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.EQ, l3Uuid);
        rq.add(NetworkServiceL3NetworkRefVO_.networkServiceType, Op.EQ, IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString());
        String providerUuid = rq.findValue();
        if (errOnException) {
            DebugUtils.Assert(providerUuid != null, String.format("the L3 network[uuid:%s] doesn't have IPsec service attached", l3Uuid));
        }

        SimpleQuery<NetworkServiceProviderVO> q = dbf.createQuery(NetworkServiceProviderVO.class);
        q.select(NetworkServiceProviderVO_.type);
        q.add(NetworkServiceProviderVO_.uuid, SimpleQuery.Op.EQ, providerUuid);
        String providerType = q.findValue();

        IPsecBackend bkd = backends.get(providerType);
        if (errOnException) {
            DebugUtils.Assert(bkd != null, String.format("no IPsecBackend for the network service provider[%s]", providerType));
        }
        return bkd;
    }

    private void handle(APICreateIPsecConnectionMsg msg) {
        APICreateIPsecConnectionEvent evt = new APICreateIPsecConnectionEvent(msg.getId());

        IPsecConnectionVO vo = new IPsecConnectionVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setState(IPsecState.Enabled);
        vo.setStatus(IPSecStatus.Connecting);
        vo.setDescription(msg.getDescription());
        vo.setAuthKey(msg.getAuthKey());
        vo.setAuthMode(msg.getAuthMode());
        vo.setIkeAuthAlgorithm(msg.getIkeAuthAlgorithm());
        vo.setIkeDhGroup(msg.getIkeDhGroup());
        vo.setIkeEncryptionAlgorithm(msg.getIkeEncryptionAlgorithm());
        vo.setPeerAddress(msg.getPeerAddress());
        vo.setPfs(msg.getPfs());
        vo.setPolicyAuthAlgorithm(msg.getPolicyAuthAlgorithm());
        vo.setPolicyMode(msg.getPolicyMode());
        vo.setTransformProtocol(msg.getTransformProtocol());
        vo.setVipUuid(msg.getVipUuid());
        vo.setPolicyEncryptionAlgorithm(msg.getPolicyEncryptionAlgorithm());
        vo.setIkeVersion(msg.getIkeVersion());
        vo.setIdType(msg.getIdType());
        vo.setLocalId(msg.getLocalId());
        vo.setRemoteId(msg.getRemoteId());
        vo.setIkeLifeTime(msg.getIkeLifeTime());
        vo.setLifeTime(msg.getLifeTime());
        vo = dbf.persistAndRefresh(vo);

        List<IPsecPeerCidrVO> peers = new ArrayList<>();
        if (msg.getPeerCidrs() != null) {
            for (String cidr : msg.getPeerCidrs()) {
                IPsecPeerCidrVO c = new IPsecPeerCidrVO();
                c.setUuid(Platform.getUuid());
                c.setCidr(cidr);
                c.setConnectionUuid(vo.getUuid());
                peers.add(c);
            }
            dbf.persistCollection(peers);
        }

        if (msg.getL3NetworkUuid() != null) {
            IPsecL3NetworkRefVO ref = new IPsecL3NetworkRefVO();
            ref.setUuid(Platform.getUuid());
            ref.setConnectionUuid(vo.getUuid());
            ref.setL3NetworkUuid(msg.getL3NetworkUuid());
            dbf.persist(ref);
        }

        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), IPsecConnectionVO.class.getSimpleName());

        /* if there is no l3 networks or peer cidrs will not call backend */
        if (msg.getL3NetworkUuid() == null || msg.getPeerCidrs() == null) {
            /* set vip usefor, it will not fail */
            ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
            vipStruct.setUseFor(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString());
            vipStruct.setServiceUuid(vo.getUuid());
            Vip vip = new Vip(msg.getVipUuid());
            vip.setStruct(vipStruct);
            vip.acquire(new Completion(msg) {
                @Override
                public void success() {
                }

                @Override
                public void fail(ErrorCode errorCode) {
                }
            });

            evt.setInventory(IPsecConnectionInventory.valueOf(dbf.reload(vo)));
            bus.publish(evt);
            return;
        }

        IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(dbf.reload(vo));
        IPsecConnectionVO finalVo = vo;
        IPsecBackend bkd = getBackend(msg.getL3NetworkUuid());
        bkd.createIPsecConnection(inv, new Completion(msg) {
            @Override
            public void success() {
                IPsecConnectionVO v = dbf.reload(finalVo);
                if (IpSecconnectionSystemTags.IPSEC_LOW_VERSION.getTag(v.getUuid(), IPsecConnectionVO.class) != null) {
                    // old ipsec status always Ready
                    v.setStatus(IPSecStatus.Ready);
                }

                if (CoreGlobalProperty.UNIT_TEST_ON) {
                    v.setStatus(IPSecStatus.Ready);
                    evt.setInventory(IPsecConnectionInventory.valueOf(v));
                    bus.publish(evt);
                    return;
                }

                try {
                    new Retry<Boolean>() {
                        String __name__ = String.format("test-ipsec-connection-%s-status", v.getUuid());
                        @Override
                        @RetryCondition(onExceptions = {RuntimeException.class}, interval = 10, times = 3)
                        protected Boolean call() {
                            if (Q.New(IPsecConnectionVO.class).eq(IPsecConnectionVO_.uuid, v.getUuid()).eq(IPsecConnectionVO_.status, IPSecStatus.Ready).isExists()) {
                                v.setStatus(IPSecStatus.Ready);
                                return true;
                            } else {
                                v.setStatus(IPSecStatus.Disconnected);
                                throw new RuntimeException(String.format("IPsec connection[%s] status down", v.getUuid()));
                            }
                        }
                    }.run();
                } catch (RuntimeException e){
                }
                evt.setInventory(IPsecConnectionInventory.valueOf(v));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                dbf.remove(finalVo);

                if (msg.getSystemTags() != null && msg.getSystemTags().contains(VipSystemTags.DELETE_ON_FAILURE.getTagFormat())) {
                    ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                    vipStruct.setUseFor(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString());
                    vipStruct.setServiceUuid(finalVo.getUuid());
                    Vip vip = new Vip(finalVo.getVipUuid());
                    vip.setStruct(vipStruct);
                    vip.release(new Completion(msg) {
                        @Override
                        public void success() {
                            bus.publish(evt);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            bus.publish(evt);
                        }
                    });
                } else {
                    bus.publish(evt);
                }
            }
        });

    }

    protected void handle(APIGetCandidateL3NetworksForIpSecConnectionMsg msg){
        APIGetCandidateL3NetworksForIpSecConnectionReply reply = new APIGetCandidateL3NetworksForIpSecConnectionReply();

        IPsecConnectionVO iPsecConnectionVO = null;
        String publicL3NetworkUuid = null;
        if (msg.getUuid() != null){
            iPsecConnectionVO = dbf.findByUuid(msg.getUuid(), IPsecConnectionVO.class);
        }

        List<String> vrUuids = new ArrayList<>();
        List<String> attachedL3Uuids = new ArrayList<>();
        if (iPsecConnectionVO != null) {
            attachedL3Uuids = iPsecConnectionVO.getL3Networks().stream().map(IPsecL3NetworkRefVO::getL3NetworkUuid).collect(Collectors.toList());
        }

        if (!attachedL3Uuids.isEmpty()) {
            /* l3 networks in same vpc router or ha group */
            vrUuids = Q.New(VmNicVO.class).in(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST)
                    .in(VmNicVO_.l3NetworkUuid, attachedL3Uuids)
                    .select(VmNicVO_.vmInstanceUuid).listValues();
        } else {
            if (msg.getVipUuid() != null) {
                vrUuids = vipProxy.getVrUuidsByNetworkService(VipVO.class.getSimpleName(), msg.getVipUuid());
            }
            if (vrUuids.isEmpty()) {
                publicL3NetworkUuid = msg.getPublicL3Uuid();
                if (publicL3NetworkUuid == null && iPsecConnectionVO != null) {
                    /* l3 networks in vpc router or ha group which has public network same as vip l3 network */
                    VipVO vipVO = dbf.findByUuid(iPsecConnectionVO.getVipUuid(), VipVO.class);
                    publicL3NetworkUuid = vipVO.getL3NetworkUuid();
                }

                vrUuids = Q.New(VmNicVO.class).in(VmNicVO_.metaData, VirtualRouterNicMetaData.PUBLIC_NIC_MASK_STRING_LIST)
                        .eq(VmNicVO_.l3NetworkUuid, publicL3NetworkUuid)
                        .select(VmNicVO_.vmInstanceUuid).listValues();
            }
        }

        List<String> candidateL3Uuids = new ArrayList<>();
        if (!vrUuids.isEmpty()) {
            candidateL3Uuids = Q.New(VmNicVO.class).in(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST)
                    .in(VmNicVO_.vmInstanceUuid, vrUuids).select(VmNicVO_.l3NetworkUuid).listValues();
            if (!candidateL3Uuids.isEmpty()) {
                candidateL3Uuids = candidateL3Uuids.stream().distinct().collect(Collectors.toList());
            }
        }

        if (!candidateL3Uuids.isEmpty() && !attachedL3Uuids.isEmpty()) {
            List<String> finalAttachedL3Uuids = new ArrayList<>(attachedL3Uuids);
            candidateL3Uuids = candidateL3Uuids.stream().filter(uuid -> !finalAttachedL3Uuids.contains(uuid)).collect(Collectors.toList());
        }

        List<L3NetworkVO> candidateL3Networks = new ArrayList<>();
        if (!candidateL3Uuids.isEmpty()) {
            String sql = "select l3 from L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref" +
                    " where l3.uuid = ref.l3NetworkUuid" +
                    " and ref.networkServiceType = :type and l3.uuid in (:uuids)" ;
            TypedQuery<L3NetworkVO> q = dbf.getEntityManager().createQuery(sql, L3NetworkVO.class);
            q.setParameter("type", IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString());
            q.setParameter("uuids", candidateL3Uuids);
            candidateL3Networks = q.getResultList();
        }

        reply.setInventories(L3NetworkInventory.valueOf(candidateL3Networks));

        bus.reply(msg, reply);
    }


    private void handle(APIGetVpcIPsecLogMsg msg) {
        APIGetVpcIPsecLogReply reply = new APIGetVpcIPsecLogReply();
        VirtualRouterAsyncHttpCallMsg vmsg = new VirtualRouterAsyncHttpCallMsg();
        VyosIPsecBackend.GetIPsecLogCmd cmd = new VyosIPsecBackend.GetIPsecLogCmd();
        cmd.lines = msg.getLines();
        vmsg.setVmInstanceUuid(msg.getUuid());
        vmsg.setCommand(cmd);
        vmsg.setPath(VyosIPsecBackend.GET_IPSEC_LOG);
        vmsg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(vmsg, VmInstanceConstant.SERVICE_ID, msg.getUuid());
        bus.send(vmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply r) {
                if (!r.isSuccess()) {
                    reply.setError(r.getError());
                    reply.setSuccess(false);
                    bus.reply(msg, reply);
                    return;
                }
                VirtualRouterAsyncHttpCallReply vrReply = r.castReply();
                reply.setIpsecLog(vrReply.toResponse(VyosIPsecBackend.GetIPsecLogRsp.class).ipsecLog);
                reply.setSuccess(true);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(IPsecConstants.SERVICE_ID);
    }

    @Override
    public RangeSet getVipUsePortRange(String vipUuid, String protocol, VipUseForList useForList){

        RangeSet portRangeList = new RangeSet();
        List<RangeSet.Range> portRanges = new ArrayList<RangeSet.Range>();

        /* ipsec need reserve the udp port 500 and 4500 */
        if (useForList.isIncluded(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString()) && protocol.equalsIgnoreCase(IPsecConstants.IPSEC_PROTOCOL_UDP)){
            portRanges.add(new RangeSet.Range(IPsecConstants.IPSEC_UDP_PORT_68, IPsecConstants.IPSEC_UDP_PORT_68, true));
            portRanges.add(new RangeSet.Range(IPsecConstants.IPSEC_UDP_PORT_500, IPsecConstants.IPSEC_UDP_PORT_500, true));
            portRanges.add(new RangeSet.Range(IPsecConstants.IPSEC_UDP_PORT_4500,IPsecConstants.IPSEC_UDP_PORT_4500, true));
        }

        portRangeList.setRanges(portRanges);

        return portRangeList;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APICreateIPsecConnectionMsg.class);
        ret.add(APIAttachL3NetworkToVmMsg.class);

        return ret;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateIPsecConnectionMsg) {
            validate((APICreateIPsecConnectionMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmMsg) {
            validate((APIAttachL3NetworkToVmMsg) msg);
        }

        return msg;
    }

    private void validate(APICreateIPsecConnectionMsg msg) {
        RangeSet portRangeList = getVipPortRangeList(msg.getVipUuid(), "UDP");

        RangeSet.Range range1 = new RangeSet.Range(IPsecConstants.IPSEC_UDP_PORT_500, IPsecConstants.IPSEC_UDP_PORT_500);
        RangeSet.Range range2 = new RangeSet.Range(IPsecConstants.IPSEC_UDP_PORT_4500, IPsecConstants.IPSEC_UDP_PORT_4500);

        for (RangeSet.Range cur : portRangeList.getRanges()) {
            if (cur.isOverlap(range1) || range1.isOverlap(cur)) {
                throw new ApiMessageInterceptionException(operr("Current port range[%s, %s] is conflicted with used port range [%s, %s] with vip[uuid: %s] protocol: UDP",
                        Long.toString(range1.getStart()), Long.toString(range1.getEnd()), Long.toString(cur.getStart()), Long.toString(cur.getEnd()), msg.getVipUuid()));
            }
            if (cur.isOverlap(range2) || range2.isOverlap(cur)) {
                throw new ApiMessageInterceptionException(operr("Current port range[%s, %s] is conflicted with used port range [%s, %s] with vip[uuid: %s] protocol: UDP",
                        Long.toString(range2.getStart()), Long.toString(range2.getEnd()), Long.toString(cur.getStart()), Long.toString(cur.getEnd()), msg.getVipUuid()));
            }
        }
    }

    private void validate(APIAttachL3NetworkToVmMsg msg) {
        /* check if virtual router vm */
        if (!dbf.isExist(msg.getVmInstanceUuid(), VirtualRouterVmVO.class)) {
            return;
        }

        List<String> vipUuids = vipProxy.getServiceUuidsByRouterUuid(msg.getVmInstanceUuid(),VipVO.class.getSimpleName());
        if (vipUuids == null || vipUuids.isEmpty()) {
            return;
        }

        /* find all ipsec of this vr */
        List<String> ipsecUuids = Q.New(IPsecConnectionVO.class).in(IPsecConnectionVO_.vipUuid, vipUuids)
                .select(IPsecConnectionVO_.uuid).listValues();
        if (ipsecUuids == null || ipsecUuids.isEmpty()) {
            return;
        }

        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class));
        List<NormalIpRangeVO> vos = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, l3Inv.getUuid())
                .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).list();
        if (vos == null || vos.isEmpty()) {
            return;
        }
        List<String> pubCidrs = vos.stream().map(NormalIpRangeVO::getNetworkCidr).collect(Collectors.toList());

        for (String uuid : ipsecUuids) {
            IPsecConnectionInventory ipsecInv = IPsecConnectionInventory.valueOf(dbf.findByUuid(uuid, IPsecConnectionVO.class));
            List<String> remoteCidrs = ipsecInv.getPeerCidrSignatures();
            for (String rCidr: remoteCidrs) {
                for (String cidr: pubCidrs) {
                    if (NetworkUtils.isCidrOverlap(cidr, rCidr)) {
                        throw new ApiMessageInterceptionException(argerr("cidr[%s] of attached L3Network [uuid:%s] is overlapped with ipsec [uuid:%s] remote cidr[%s]",
                                cidr, l3Inv.getUuid(), uuid, rCidr));
                    }
                }
            }
        }
    }

    private RangeSet getVipPortRangeList(String vipUuid, String protocol){
        VipUseForList vipUseForList = new VipUseForList();
        List<RangeSet.Range> portRangeList = new ArrayList<RangeSet.Range>();
        for (VipGetUsedPortRangeExtensionPoint ext : pluginRgty.getExtensionList(VipGetUsedPortRangeExtensionPoint.class)){
            RangeSet range = ext.getVipUsePortRange(vipUuid, protocol, vipUseForList);
            portRangeList.addAll(range.getRanges());
        }

        RangeSet portRange = new RangeSet();
        portRange.setRanges(portRangeList);
        portRange.sort();
        return portRange;
    }

    @Override
    public ServiceReference getServiceReference(String vipUuid) {
        List<String> uuids = SQL.New("select distinct cn.uuid" +
                " from IPsecConnectionVO cn, IPsecL3NetworkRefVO ref, IPsecPeerCidrVO peer" +
                " where cn.vipUuid = :vipUuid and cn.uuid = ref.connectionUuid" +
                " and cn.uuid = peer.connectionUuid").param("vipUuid", vipUuid).list();

        if (uuids != null && !uuids.isEmpty()) {
            long count = Q.New(IPsecL3NetworkRefVO.class).in(IPsecL3NetworkRefVO_.connectionUuid, uuids).count();
            return new VipGetServiceReferencePoint.ServiceReference(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString(), count, uuids);
        }
        return new VipGetServiceReferencePoint.ServiceReference(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString(), 0, new ArrayList<>());
    }

    @Override
    public ServiceReference getServicePeerL3Reference(String vipUuid, String peerL3Uuid) {
        List<String> uuids = SQL.New("select distinct cn.uuid" +
                " from IPsecConnectionVO cn, IPsecL3NetworkRefVO ref" +
                " where cn.vipUuid = :vipUuid and cn.uuid = ref.connectionUuid" +
                " and ref.l3NetworkUuid = :l3uuid")
                        .param("vipUuid", vipUuid).param("l3uuid", peerL3Uuid).list();

        if (uuids == null ) {
            uuids =  new ArrayList<>();
        }
        return new VipGetServiceReferencePoint.ServiceReference(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString(), uuids.size(), uuids);
    }

    private void syncIPsecConnnection(L3NetworkInventory inventory, String vrUuid, Completion completion){
        List<IPsecL3NetworkRefVO> vos = Q.New(IPsecL3NetworkRefVO.class).eq(IPsecL3NetworkRefVO_.l3NetworkUuid, inventory.getUuid()).list();
        if (vos == null || vos.isEmpty()) {
            completion.success();
            return;
        }

        List<ErrorCode> errs = new ArrayList<>();
        new While<>(vos).each((vo, cmpl) -> {
            dbf.remove(vo);
            IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(dbf.findByUuid(vo.getConnectionUuid(), IPsecConnectionVO.class));
            if (inv.getPeerCidrSignatures() == null || inv.getPeerCidrSignatures().isEmpty()) {
                cmpl.done();
                return;
            }

            IPsecConnectionSyncMsg msg = new IPsecConnectionSyncMsg();
            msg.setInv(inv);
            msg.setL3NetworkUuid(inventory.getUuid());
            VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(vrUuid, VirtualRouterVmVO.class));
            msg.setVr(vrInv);
            msg.setSkip_vip_release(true);
            bus.makeTargetServiceIdByResourceUuid(msg, IPsecConstants.SERVICE_ID, vo.getConnectionUuid());
            bus.send(msg, new CloudBusCallBack(cmpl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errs.add(reply.getError());
                    }
                    cmpl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion){
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errs.isEmpty()){
                    completion.success();
                } else {
                    completion.fail(errs.get(0));
                }
            }
        });
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class));
        syncIPsecConnnection(l3Inv, nic.getVmInstanceUuid(), completion);
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }
}

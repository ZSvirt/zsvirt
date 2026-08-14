package org.zstack.ipsec.vyos;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.ApplianceVmHaStatus;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.appliancevm.ApplianceVmVO_;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.NormalIpRangeVO;
import org.zstack.header.network.l3.NormalIpRangeVO_;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.ipsec.*;
import org.zstack.ipsec.vyos.VyosIPsecConstants.Param;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipReleaseExtensionPoint;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.vip.VipConfigProxy;
import org.zstack.network.service.virtualrouter.vyos.*;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static org.zstack.appliancevm.ApplianceVmConstant.APPLIANCE_VM_TYPE;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.ipsec.IPsecConstants.IPSEC_STATE_DOWN;
import static org.zstack.ipsec.IPsecConstants.IPSEC_STATE_UP;

/**
 * Created by xing5 on 2016/11/8.
 */
public class VyosIPsecBackend extends AbstractVirtualRouterBackend implements IPsecBackend,
        VyosPostCreateFlowExtensionPoint, VyosPostRebootFlowExtensionPoint, VyosPostStartFlowExtensionPoint,
        VyosProvisionConfigFlowExtensionPoint, VipReleaseExtensionPoint, Component, GlobalApiMessageInterceptor, VirtualRouterHaGetCallbackExtensionPoint,
        VirtualRouterTrackerExtensionPoint, VmInstanceStopExtensionPoint, VmInstanceRebootExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VyosIPsecBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    private VirtualRouterHaBackend haBackend;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    protected VipConfigProxy vipProxy;
    @Autowired
    protected TagManager tagMgr;

    public static final String CREATE_IPSEC_CONNECTION = "/vyos/createipsecconnection";
    public static final String DELETE_IPSEC_CONNECTION = "/vyos/deleteipsecconnection";
    public static final String UPDATE_IPSEC_CONNECTION = "/vyos/updateipsecconnection";
    public static final String SYNC_IPSEC_CONNECTION   = "/vyos/syncipsecconnection";
    public static final String UPDATE_IPSEC_VERSION = "/vyos/updateipsecversion";
    public static final String GET_IPSEC_LOG = "/vyos/getipseclog";

    public static final String CHANGE_IPSEC_TASK = "changeIPsecState";
    public static final String SYNC_IPSEC_TASK = "syncIPsec";
    public static final String DELETE_IPSEC_TASK = "deleteIPsec";
    public static final String CREATE_IPSEC_TASK = "createIPsec";
    public static final String UPDATE_IPSEC_VERSION_TASK = "updateIPsecVersion";

    private List<String> createIPsecFlowNames;
    private List<String> deleteIPsecFlowNames;
    private FlowChainBuilder createIPsecFlowBuilder;
    private FlowChainBuilder deleteIPsecFlowBuilder;

    public void setCreateIPsecFlowNames(List<String> createIPsecFlowNames) {
        this.createIPsecFlowNames = createIPsecFlowNames;
    }

    public void setDeleteIPsecFlowNames(List<String> deleteIPsecFlowNames) {
        this.deleteIPsecFlowNames = deleteIPsecFlowNames;
    }

    private Flow createSyncFlow() {
        return new VyosIPsecSyncFlow();
    }

    @Override
    public Flow vyosPostStartFlow() {
        return createSyncFlow();
    }

    @Override
    public Flow vyosPostCreateFlow() {
        return createSyncFlow();
    }

    @Override
    public Flow vyosPostRebootFlow() {
        return createSyncFlow();
    }

    @Override
    public Flow vyosProvisionConfigFlow() {
        return createSyncFlow();
    }

    @Override
    public boolean start() {
        createIPsecFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(createIPsecFlowNames).construct();
        deleteIPsecFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(deleteIPsecFlowNames).construct();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getVipUse() {
        return IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString();
    }

    /* it's better to do check in interceptor, BUT it's more complicated than this method, because:
     * if virtual router is not created, interceptor need to find the virtualrouter offering to get pub, mngt network,
     * */
    private void checkRemoteCidrOverlappedWithPublicNetwork(IPsecConnectionInventory inv, String vrUuid) {
        List<String> remoteCidrs = inv.getPeerCidrSignatures();
        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(dbf.findByUuid(vrUuid, VirtualRouterVmVO.class));

        for (VmNicInventory nic : vr.getVmNics()) {
            List<NormalIpRangeVO> vos = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, nic.getL3NetworkUuid()).eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).list();
            if (vos == null || vos.isEmpty()) {
                continue;
            }

            for (NormalIpRangeVO vo : vos) {
                String cidr = vo.getNetworkCidr();
                for (String rcidr : remoteCidrs) {
                    if (NetworkUtils.isCidrOverlap(cidr, rcidr)) {
                        throw new OperationFailureException(operr("the remoteCidr[%s] is overlaped with VirtualRouter interface cidr[%s]",
                                rcidr, cidr));
                    }
                }
            }
        }
    }

    private void releaseServicesOnVip(final Iterator<IPsecConnectionVO> vos, VipInventory vip, Completion completion) {
        if (!vos.hasNext()) {
            completion.success();
            return;
        }

        IPsecConnectionVO vo = vos.next();
        IPsecConnectionInventory inv = IPsecConnectionInventory.valueOf(vo);
        if (inv.getLocalL3Networks().isEmpty() || inv.getPeerCidrSignatures().isEmpty()) {
            dbf.removeCollection(vo.getPeerCidrs(), IPsecPeerCidrVO.class);
            dbf.removeByPrimaryKey(vo.getUuid(), IPsecConnectionVO.class);
            releaseServicesOnVip(vos, vip, completion);
            return;
        }

        if (!vrMgr.isVirtualRouterRunningForL3Network(inv.getLocalL3Networks().get(0))) {
            logger.debug(String.format("the vyos vm is not running for the L3 network[uuid%s], skip deleting IPsec connection", inv.getLocalL3Networks().get(0)));
            dbf.removeCollection(vo.getPeerCidrs(), IPsecPeerCidrVO.class);
            dbf.removeByPrimaryKey(vo.getUuid(), IPsecConnectionVO.class);
            releaseServicesOnVip(vos, vip, completion);
            return;
        }

        // Note(WeiW): Since the outer flow will release vip, do not need to run releaseVipFlow included in deleteIPsecFlow
        List<L3NetworkInventory> l3Invs = inv.getLocalL3Networks().stream().map(uuid -> L3NetworkInventory.valueOf(dbf.findByUuid(uuid, L3NetworkVO.class)))
                .collect(Collectors.toList());
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(l3Invs.get(0).getUuid(), IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE);
        VirtualRouterVmInventory vr = vrMgr.getVirtualRouterVm(l3Invs.get(0));

        Map data = new HashMap();
        data.put(Param.VIP, vip);
        data.put(Param.GUEST_L3, l3Invs);
        data.put(Param.IPSEC_STRUCT, inv);
        data.put(Param.UNLOCK_VIP, true);
        data.put(Param.SERVICE_PROVIDER_TYPE, providerType.toString());
        data.put(Param.VR, vr);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setData(data);
        chain.setName(String.format("delete-ipsec-connection-%s-for-vip-%s", inv.getUuid(), vip.getUuid()));
        chain.then(new VyosDeleteIPsecFlow());
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                dbf.removeCollection(vo.getPeerCidrs(), IPsecPeerCidrVO.class);
                dbf.removeByPrimaryKey(vo.getUuid(), IPsecConnectionVO.class);
                releaseServicesOnVip(vos, vip, completion);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void releaseServicesOnVip(VipInventory vip, Completion completion) {

        List<IPsecConnectionVO> vos = Q.New(IPsecConnectionVO.class).eq(IPsecConnectionVO_.vipUuid, vip.getUuid()).list();
        if (vos.isEmpty()) {
            completion.success();
            return;
        }

        releaseServicesOnVip(vos.iterator(), vip, completion);
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(APICreateIPsecConnectionMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateIPsecConnectionMsg) {
            validate((APICreateIPsecConnectionMsg) msg);
        }

        return msg;
    }

    @Transactional(readOnly = true)
    private void validate(APICreateIPsecConnectionMsg msg) {
        if (msg.getL3NetworkUuid() == null) {
            return;
        }

        String sql = "select count(*) from NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO pro where" +
                " ref.networkServiceProviderUuid = pro.uuid and ref.l3NetworkUuid = :l3Uuid and pro.type = :proType" +
                " and ref.networkServiceType = :serviceType";

        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("l3Uuid", msg.getL3NetworkUuid());
        q.setParameter("proType", VyosConstants.PROVIDER_TYPE.toString());
        q.setParameter("serviceType", IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString());
        long count = q.getSingleResult();
        if (count < 1) {
            return;
        }

        if ("aes-192".equals(msg.getIkeEncryptionAlgorithm())) {
            throw new ApiMessageInterceptionException(argerr(
                    "vyos doesn't support aes-192 as IkeEncryptionAlgorithm, available options aes-128, aes-256, 3des"
            ));
        }

        if ("aes-192".equals(msg.getPolicyEncryptionAlgorithm())) {
            throw new ApiMessageInterceptionException(argerr(
                    "vyos doesn't support aes-192 as PolicyEncryptionAlgorithm, available options aes-128, aes-256, 3des"
            ));
        }

        if(IntStream.of(IPsecConstants.IKE_DH_GROUP).noneMatch(x -> x == msg.getIkeDhGroup())){
            throw new ApiMessageInterceptionException(argerr(
                    "vyos doesn't support %d as Ike DhGroup ",msg.getIkeDhGroup()
            ));
        }
    }

    public static class IPsecInfo {
        public String uuid;
        public String state;
        public List<String> localCidrs;
        public String peerAddress;
        public String authMode;
        public String authKey;
        public String vip;
        public String publicNic;
        public String ikeAuthAlgorithm;
        public String ikeEncryptionAlgorithm;
        public int ikeDhGroup;
        public String policyAuthAlgorithm;
        public String policyEncryptionAlgorithm;
        public String pfs;
        public String policyMode;
        public String transformProtocol;
        public List<String> peerCidrs;
        public boolean excludeSnat;
        public List<String> modifiedItems;
        public String ikeVersion;
        public String idType;
        public String localId;
        public String remoteId;
        public int ikeLifeTime;
        public int lifeTime;
    }

    public static class UpdateIPsecVersionCmd extends VirtualRouterCommands.AgentCommand {
        public List<IPsecInfo> infos;
        public Boolean         autoRestartVpn;
        public String          targetVersion;
    }

    public static class GetIPsecLogCmd extends VirtualRouterCommands.AgentCommand {
        public int             lines;
    }

    public static class GetIPsecLogRsp extends VirtualRouterCommands.AgentResponse {
        public String          ipsecLog;
    }

    public static class CreateIPsecConnectionCmd extends VirtualRouterCommands.AgentCommand {
        public List<IPsecInfo> infos;
        public Boolean         autoRestartVpn;
    }

    public static class CreateIPsecConnectionRsp extends VirtualRouterCommands.AgentResponse {
    }

    public static class DeleteIPsecConnectionCmd extends VirtualRouterCommands.AgentCommand {
        public List<IPsecInfo> infos;
    }

    public static class DeleteIPsecConnectionRsp extends VirtualRouterCommands.AgentResponse {
    }

    public static class UpdateIPsecConnectionCmd extends VirtualRouterCommands.AgentCommand {
        public List<IPsecInfo> infos;
    }

    public static class UpdateIPsecConnectionRsp extends VirtualRouterCommands.AgentResponse {
    }

    public static class SyncIPsecConnectionCmd extends VirtualRouterCommands.AgentCommand {
        public List<IPsecInfo> infos;
        public Boolean         autoRestartVpn;
        public Boolean         needStatus;
    }

    public static class SyncIPsecConnectionRsp extends VirtualRouterCommands.AgentResponse {
        public List<String> downIpsecConns;
    }

    protected IPsecInfo createIPsecInfo(IPsecConnectionInventory inv, VirtualRouterVmInventory vr) {
        IPsecInfo info = new IPsecInfo();
        info.uuid = inv.getUuid();
        info.peerAddress = inv.getPeerAddress();
        info.authMode = inv.getAuthMode();
        info.authKey = inv.getAuthKey();
        info.ikeAuthAlgorithm = inv.getIkeAuthAlgorithm();
        info.ikeAuthAlgorithm = inv.getIkeAuthAlgorithm();
        info.ikeEncryptionAlgorithm = inv.getIkeEncryptionAlgorithm().replaceAll("-", "");
        info.ikeDhGroup = inv.getIkeDhGroup();
        info.policyAuthAlgorithm = inv.getPolicyAuthAlgorithm();
        info.policyEncryptionAlgorithm = inv.getPolicyEncryptionAlgorithm().replaceAll("-", "");
        info.pfs = inv.getPfs();
        info.policyMode = inv.getPolicyMode();
        info.transformProtocol = inv.getTransformProtocol();
        info.peerCidrs = inv.getPeerCidrSignatures();

        VipVO vipVO = dbf.findByUuid(inv.getVipUuid(), VipVO.class);
        info.vip = vipVO.getIp();
        info.localCidrs = new ArrayList<>(inv.getLocalL3Cidrs());
        for (VmNicInventory nic : vr.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(vipVO.getL3NetworkUuid())) {
                info.publicNic = nic.getMac();
                break;
            }
        }

        info.ikeVersion = inv.getIkeVersion();
        info.idType = inv.getIdType();
        info.localId = inv.getLocalId();
        info.remoteId = inv.getRemoteId();
        if (StringUtils.isEmpty(info.remoteId)) {
            info.remoteId = IpSecconnectionSystemTags.IPSEC_PEER_REMOTE_ID.getTokenByResourceUuid(info.uuid,
                    IpSecconnectionSystemTags.IPSEC_PEER_REMOTE_ID_TOKEN);
        }
        info.ikeLifeTime = inv.getIkeLifeTime();
        info.lifeTime = inv.getLifeTime();

        info.excludeSnat = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                for (String l3Uuid : inv.getLocalL3Networks()) {
                    L3NetworkInventory l3network = L3NetworkInventory.valueOf(dbf.findByUuid(l3Uuid, L3NetworkVO.class));
                    for (NetworkServiceL3NetworkRefInventory ref : l3network.getNetworkServices()) {
                        if (ref.getNetworkServiceType().equals(NetworkServiceType.SNAT.toString())) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }.call();

        return info;
    }

    @Override
    public void syncIPsecConnection(IPsecConnectionInventory inv, Completion completion) {
        if (inv.getL3NetworkRefs() == null || inv.getL3NetworkRefs().isEmpty()) {
            completion.success();
            return;
        }

        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(inv.getL3NetworkRefs().get(0).getL3NetworkUuid(), L3NetworkVO.class));
        VirtualRouterVmInventory vrInv = vrMgr.getVirtualRouterVm(l3Inv);
        if (vrInv == null) {
            completion.success();
            return;
        }

        try {
            checkRemoteCidrOverlappedWithPublicNetwork(inv, vrInv.getUuid());
        } catch (RuntimeException e) {
            completion.fail(operr(e.getMessage()));
            return;
        }

        syncIPsecConnection(vrInv, asList(inv), new Completion(completion) {
            @Override
            public void success() {
                submitSyncIPsecConnectionToHaRouter(vrInv, asList(inv), new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(operr("sync to ha group failed, because:%s", errorCode.getDescription()));
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void submitSyncIPsecConnectionToHaRouter(VirtualRouterVmInventory vrInv, List<IPsecConnectionInventory> invs, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(SYNC_IPSEC_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(invs));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    public void syncIPsecConnection(VirtualRouterVmInventory vrInv, List<IPsecConnectionInventory> invs, Completion completion) {
        SyncIPsecConnectionCmd cmd = new SyncIPsecConnectionCmd();
        cmd.infos = invs.stream().map(inv -> createIPsecInfo(inv, vrInv)).collect(Collectors.toList());
        cmd.autoRestartVpn = rcf.getResourceConfigValue(VyosGlobalConfig.AUTO_RESTART_IPSEC, vrInv.getUuid(), Boolean.class);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vrInv.getUuid());
        msg.setPath(SYNC_IPSEC_CONNECTION);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrInv.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                VirtualRouterAsyncHttpCallReply ar = reply.castReply();
                SyncIPsecConnectionRsp rsp = ar.toResponse(SyncIPsecConnectionRsp.class);
                if (!rsp.isSuccess()) {
                    throw new OperationFailureException(operr("operation error, because:%s", rsp.getError()));
                }

                List<String> downList = new ArrayList<>();
                if (rsp.downIpsecConns == null || rsp.downIpsecConns.isEmpty()) {
                    completion.success();
                    return;
                }
                invs.forEach( ipsec -> {
                    if (ipsec.getStatus().equals(IPSecStatus.Ready.toString()) && rsp.downIpsecConns.contains(ipsec.getUuid())) {
                        downList.add(ipsec.getUuid());
                    }
                });
                if(!downList.isEmpty()) {
                    SQL.New(IPsecConnectionVO.class)
                            .in(IPsecConnectionVO_.uuid, downList)
                            .set(IPsecConnectionVO_.status, IPSecStatus.Disconnected).update();
                }
                completion.success();
            }
        });
    }

    @Override
    public void createIPsecConnection(IPsecConnectionInventory inv, Completion completion) {
        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(inv.getL3NetworkRefs().get(0).getL3NetworkUuid(), L3NetworkVO.class));
        VirtualRouterVmInventory vrInv = vrMgr.getVirtualRouterVm(l3Inv);
        if (vrInv != null) {
            try {
                checkRemoteCidrOverlappedWithPublicNetwork(inv, vrInv.getUuid());
            } catch (RuntimeException e) {
                completion.fail(operr(e.getMessage()));
                return;
            }
        }


        List<L3NetworkInventory> L3Networks = inv.getLocalL3Networks().stream()
                .map(uuid -> L3NetworkInventory.valueOf(dbf.findByUuid(uuid, L3NetworkVO.class))).collect(Collectors.toList());
        VipInventory vip = VipInventory.valueOf(dbf.findByUuid(inv.getVipUuid(), VipVO.class));
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(L3Networks.get(0).getUuid(),
                IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE);
        Map data = new HashMap();
        data.put(Param.GUEST_L3, L3Networks);
        data.put(Param.VIP, vip);
        data.put(Param.IPSEC_STRUCT, inv);
        data.put(Param.SERVICE_PROVIDER_TYPE, providerType.toString());

        FlowChain chain = createIPsecFlowBuilder.build();
        chain.setName(String.format("create-ipsec-connection-%s:%s-vip-%s", inv.getName(), inv.getUuid(), vip.getUuid()));
        chain.setData(data);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void deleteIPsecConnection(IPsecConnectionInventory inv, String l3NetworkUuid, VirtualRouterVmInventory vrInv, boolean skip_vip, NoErrorCompletion completion) {
        VipInventory vip = VipInventory.valueOf(dbf.findByUuid(inv.getVipUuid(), VipVO.class));
        IPsecInfo info = createIPsecInfo(inv, vrInv);
        NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(l3NetworkUuid, IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE);

        Map data = new HashMap();
        data.put(Param.IPSEC_INFO, info);
        data.put(Param.VIP, vip);
        data.put(Param.IPSEC_STRUCT, inv);
        data.put(Param.UNLOCK_VIP, true);
        data.put(Param.SERVICE_PROVIDER_TYPE, providerType.toString());
        data.put(Param.SKIP_VIP, skip_vip);
        data.put(Param.VR, vrInv);

        FlowChain chain = deleteIPsecFlowBuilder.build();
        chain.setData(data);
        chain.setName(String.format("delete-ipsec-connection-%s:%s-vip-%s", inv.getName(), inv.getUuid(), vip.getUuid()));
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.done();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.done();
            }
        }).start();
    }

    private void submitChangeIPsecConnectionStateToHaRouter(VirtualRouterVmInventory vrInv, List<IPsecConnectionInventory> invs, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(CHANGE_IPSEC_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(invs));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    private void changeBackednIPsecConnectionState(VirtualRouterVmInventory vr, List<IPsecInfo> infos, Completion completion) {
        UpdateIPsecConnectionCmd cmd = new UpdateIPsecConnectionCmd();
        cmd.infos = infos;

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VyosIPsecBackend.UPDATE_IPSEC_CONNECTION);
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setCommand(cmd);
        msg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply ar = reply.castReply();
                UpdateIPsecConnectionRsp rsp = ar.toResponse(UpdateIPsecConnectionRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("operation error, because:%s", rsp.getError()));
                } else {
                    completion.success();
                }
            }
        });
    }

    @Override
    public void changeIPsecConnectionState(IPsecConnectionInventory inv, IPsecState nextState, final Completion completion){
        if (!vrMgr.isVirtualRouterRunningForL3Network(inv.getLocalL3Networks().get(0))) {
            logger.debug(String.format("the vyos vm is not running for the L3 network[uuid%s], skip change ipsec state", inv.getLocalL3Networks().get(0)));
            completion.fail(new ErrorCode());
            return;
        }

        L3NetworkInventory l3 = L3NetworkInventory.valueOf(dbf.findByUuid(inv.getLocalL3Networks().get(0), L3NetworkVO.class));
        VirtualRouterVmInventory vr = vrMgr.getVirtualRouterVm(l3);
        if (vr == null) {
            completion.success();
            return;
        }

        IPsecInfo info = createIPsecInfo(inv, vr);
        info.modifiedItems = asList("State");
        info.state = nextState.toString();

        changeBackednIPsecConnectionState(vr, asList(info), new Completion(completion) {
            @Override
            public void success() {
                submitChangeIPsecConnectionStateToHaRouter(vr, asList(inv), new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(operr("apply to ha group failed, because %s", errorCode.getDescription()));
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public String getNetworkServiceProviderType() {
        return VyosConstants.VYOS_ROUTER_PROVIDER_TYPE;
    }

    @Override
    public void deleteIpSecconnection(VirtualRouterVmInventory vr, IPsecConnectionInventory inv, Completion completion) {
        DeleteIPsecConnectionCmd cmd = new DeleteIPsecConnectionCmd();
        IPsecInfo info = createIPsecInfo(inv, vr);
        cmd.infos = asList(info);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VyosIPsecBackend.DELETE_IPSEC_CONNECTION);
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setCommand(cmd);
        msg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    // TODO: add GC
                    throw new OperationFailureException(reply.getError());
                }

                VirtualRouterAsyncHttpCallReply ar = reply.castReply();
                DeleteIPsecConnectionRsp rsp = ar.toResponse(DeleteIPsecConnectionRsp.class);
                if (!rsp.isSuccess()) {
                    throw new OperationFailureException(operr("operation error, because:%s", rsp.getError()));
                }

                fireFirewallEvent(vr.getUuid());
                completion.success();
            }
        });
    }

    private void fireFirewallEvent(String vRouterUuid) {
        FirewallCanonicalEvents.FirewallRuleChangedData data = new FirewallCanonicalEvents.FirewallRuleChangedData();
        data.setVirtualRouterUuid(vRouterUuid);
        evtf.fire(FirewallCanonicalEvents.FIREWALL_RULE_CHANGED_PATH, data);
    }

    @Override
    public void createIpsecConnection(VirtualRouterVmInventory vr, IPsecConnectionInventory inv, Completion completion) {
        CreateIPsecConnectionCmd cmd = new CreateIPsecConnectionCmd();
        IPsecInfo info = createIPsecInfo(inv, vr);

        cmd.infos = asList(info);
        cmd.autoRestartVpn = rcf.getResourceConfigValue(VyosGlobalConfig.AUTO_RESTART_IPSEC, vr.getUuid(), Boolean.class);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setCommand(cmd);
        msg.setPath(VyosIPsecBackend.CREATE_IPSEC_CONNECTION);
        msg.setCheckStatus(true);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply ar = reply.castReply();
                CreateIPsecConnectionRsp rsp = ar.toResponse(CreateIPsecConnectionRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("operation error, because:%s", rsp.getError()));
                    return;
                }

                fireFirewallEvent(vr.getUuid());
                completion.success();
            }
        });
    }
    @Override
    public void updateIpsecVersion(String vrUuid, String targetVersion, Completion completion) {

        if (!dbf.isExist(vrUuid, VirtualRouterVmVO.class)) {
            completion.fail(operr("update ipsec version failed, because:vpc[%s] not exist", vrUuid));
            return;
        }
        /* find all ipsec of this vr */
        List<String> vipUuids = vipProxy.getServiceUuidsByRouterUuid(vrUuid, VipVO.class.getSimpleName());
        List<IPsecConnectionInventory> invs;
        List<IPsecConnectionVO> ipsecVos = Q.New(IPsecConnectionVO.class).in(IPsecConnectionVO_.vipUuid, vipUuids).list();
        invs = IPsecConnectionInventory.valueOf(ipsecVos);

        updateIpsecVersion(vrUuid, targetVersion, invs, new Completion(completion) {
            @Override
            public void success() {
                submitUpdateIPsecVersionToHaRouter(vrUuid, targetVersion, new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(operr("sync to ha group failed, because:%s", errorCode.getDescription()));
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void submitUpdateIPsecVersionToHaRouter(String vrUuid, String targetVersion, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(UPDATE_IPSEC_VERSION_TASK);
        task.setOriginRouterUuid(vrUuid);
        task.setJsonData(JSONObjectUtil.toJsonString(targetVersion));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    public void updateIpsecVersion(String vrUuid, String targetVersion, List<IPsecConnectionInventory> invs, Completion completion) {
        UpdateIPsecVersionCmd cmd = new UpdateIPsecVersionCmd();
        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(vrUuid, VirtualRouterVmVO.class));

        cmd.targetVersion = targetVersion;
        cmd.infos = invs.stream().map(inv -> createIPsecInfo(inv, vrInv)).collect(Collectors.toList());
        cmd.autoRestartVpn = rcf.getResourceConfigValue(VyosGlobalConfig.AUTO_RESTART_IPSEC, vrInv.getUuid(), Boolean.class);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vrInv.getUuid());
        msg.setPath(UPDATE_IPSEC_VERSION);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrInv.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                VirtualRouterAsyncHttpCallReply ar = reply.castReply();
                if (!ar.isSuccess()) {
                    throw new OperationFailureException(operr("operation error, because:%s", ar.getError()));
                }

                VirtualRouterSoftwareVersionVO vrm = Q.New(VirtualRouterSoftwareVersionVO.class)
                        .eq(VirtualRouterSoftwareVersionVO_.uuid, vrInv.getUuid())
                        .eq(VirtualRouterSoftwareVersionVO_.softwareName, "IPsec")
                        .find();
                vrm.setCurrentVersion(cmd.targetVersion);
                dbf.updateAndRefresh(vrm);

                if (targetVersion.equals(vrm.getLatestVersion())) {
                    List<String> l3Uuids = vrInv.getGuestL3Networks();
                    if (!l3Uuids.isEmpty()) {
                        List<IPsecConnectionVO> ipsecs = SQL.New("select distinct ipsec from IPsecConnectionVO ipsec, IPsecL3NetworkRefVO ref " +
                                        "where ipsec.uuid=ref.connectionUuid and ref.l3NetworkUuid in (:l3Uuids)", IPsecConnectionVO.class)
                                .param("l3Uuids", l3Uuids).list();
                        ipsecs.forEach(ipsec -> {
                            if (IpSecconnectionSystemTags.IPSEC_LOW_VERSION.getTag(ipsec.getUuid(), IPsecConnectionVO.class) != null) {
                                IpSecconnectionSystemTags.IPSEC_LOW_VERSION.deleteInherentTag(ipsec.getUuid(), IPsecConnectionVO.class);
                            }
                        });
                    }
                }
                completion.success();
            }
        });
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct updateIpsecVersion = new VirtualRouterHaCallbackStruct();
        updateIpsecVersion.type = UPDATE_IPSEC_VERSION_TASK;
        updateIpsecVersion.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("ha router[%s] does not exist, can not call change ipsec state", vrUuid));
                    completion.success();
                    return;
                }

                String targetVersion = JSONObjectUtil.toObject(task.getJsonData(), String.class);
                /* find all ipsec of this vr */
                List<String> vipUuids = vipProxy.getServiceUuidsByRouterUuid(vrUuid,VipVO.class.getSimpleName());
                List<IPsecConnectionInventory> invs;
                List<IPsecConnectionVO> ipsecVos = Q.New(IPsecConnectionVO.class).in(IPsecConnectionVO_.vipUuid, vipUuids).list();
                invs = IPsecConnectionInventory.valueOf(ipsecVos);
                updateIpsecVersion(vrUuid, targetVersion, invs, completion);
            }
        };
        structs.add(updateIpsecVersion);

        VirtualRouterHaCallbackStruct changeIpSecState = new VirtualRouterHaCallbackStruct();
        changeIpSecState.type = CHANGE_IPSEC_TASK;
        changeIpSecState.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("ha router[%s] does not exist, can not call change ipsec state", vrUuid));
                    completion.success();
                    return;
                }

                IPsecConnectionInventory[] iinvs = JSONObjectUtil.toObject(task.getJsonData(), IPsecConnectionInventory[].class);
                VirtualRouterVmInventory vrInvv = VirtualRouterVmInventory.valueOf(vrVO);
                List<IPsecInfo> infos = new ArrayList<>();
                for (IPsecConnectionInventory inv : iinvs) {
                    IPsecInfo info = createIPsecInfo(inv, vrInvv);
                    infos.add(info);
                }

                changeBackednIPsecConnectionState(vrInvv, infos, completion);
            }
        };
        structs.add(changeIpSecState);

        VirtualRouterHaCallbackStruct syncIpsec = new VirtualRouterHaCallbackStruct();
        syncIpsec.type = SYNC_IPSEC_TASK;
        syncIpsec.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
                if (vrVO == null) {
                    logger.debug(String.format("ha router[%s] does not exist, can not call sync ipsec", vrUuid));
                    completion.success();
                    return;
                }

                /* data in call is not same to paras in submitChangeIPsecConnectionStateToHaRouter */
                IPsecConnectionInventory[] invs = JSONObjectUtil.toObject(task.getJsonData(), IPsecConnectionInventory[].class);
                syncIPsecConnection(VirtualRouterVmInventory.valueOf(vrVO), Arrays.asList(invs), completion);
            }
        };
        structs.add(syncIpsec);

        VirtualRouterHaCallbackStruct deleteIpsec = new VirtualRouterHaCallbackStruct();
        deleteIpsec.type = DELETE_IPSEC_TASK;
        deleteIpsec.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO =  Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrUuid).find();
                if (vrVO == null) {
                    logger.debug(String.format("ha router[%s] does not exist, can not call delete ipsec", vrUuid));
                    completion.success();
                    return;
                }

                IPsecConnectionInventory iinv = JSONObjectUtil.toObject(task.getJsonData(), IPsecConnectionInventory.class);
                deleteIpSecconnection(VirtualRouterVmInventory.valueOf(vrVO), iinv, completion);
            }
        };
        structs.add(deleteIpsec);

        VirtualRouterHaCallbackStruct createIpsec = new VirtualRouterHaCallbackStruct();
        createIpsec.type = CREATE_IPSEC_TASK;
        createIpsec.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrUuid).find();
                if (vrVO == null) {
                    logger.debug(String.format("ha router[%s] does not exist, can not call create ipsec", vrUuid));
                    completion.success();
                    return;
                }

                IPsecConnectionInventory iinv = JSONObjectUtil.toObject(task.getJsonData(), IPsecConnectionInventory.class);
                createIpsecConnection(VirtualRouterVmInventory.valueOf(vrVO), iinv, completion);
            }
        };
        structs.add(createIpsec);

        return structs;
    }

    @Override
    public void handleTracerReply(String resourceUuid, MessageReply mreply) {
        if (!(mreply instanceof PingVirtualRouterVmReply)) {
            return;
        }
        if (!mreply.isSuccess()) {
            return;
        }
        PingVirtualRouterVmReply reply = (PingVirtualRouterVmReply) mreply;
        if (ApplianceVmHaStatus.Backup.toString().equals(reply.getHaStatus())) {
            return;
        }
        if (reply.getServiceHealthList() == null || reply.getServiceHealthList().get(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString()) == null) {
            return;
        }
        List<String> ipsecConns = new ArrayList<>(reply.getServiceHealthList().get(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString()).keySet());
        if (ipsecConns.size() == 0) {
            return;
        }
        List<IPsecConnectionVO> vos = Q.New(IPsecConnectionVO.class).in(IPsecConnectionVO_.uuid, ipsecConns).list();
        vos.forEach(ipsecConn -> {
            if (IPSEC_STATE_UP.equals(reply.getServiceHealthList().get(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString()).get(ipsecConn.getUuid()))) {
                ipsecConn.setStatus(IPSecStatus.Ready);
            } else if (IPSEC_STATE_DOWN.equals(reply.getServiceHealthList().get(IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString()).get(ipsecConn.getUuid()))){
                ipsecConn.setStatus(IPSecStatus.Disconnected);
            } else {
                logger.debug(String.format("IPSec conn: [%s] state was missing", ipsecConn.getUuid()));
            }
        });
        dbf.updateCollection(vos);
    }

    @Override
    public String preStopVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStopVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterStopVm(VmInstanceInventory inv) {
        changeIPsecConnStatus(inv, IPSecStatus.Disconnected);
    }

    @Override
    public void failedToStopVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    private void changeIPsecConnStatus(VmInstanceInventory inv, IPSecStatus ipsecStatus) {
        if (APPLIANCE_VM_TYPE.equals(inv.getType())) {
            if (!Q.New(ApplianceVmVO.class)
                    .eq(ApplianceVmVO_.uuid, inv.getUuid())
                    .eq(ApplianceVmVO_.haStatus, ApplianceVmHaStatus.Backup)
                    .isExists()) {
                VirtualRouterVmVO vr = dbf.findByUuid(inv.getUuid(), VirtualRouterVmVO.class);
                VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(vr);
                List<String> l3Uuids = vrInv.getGuestL3Networks();
                if (l3Uuids.isEmpty()) {
                    return;
                }
                List<IPsecConnectionVO> ipsecs = SQL.New("select distinct ipsec from IPsecConnectionVO ipsec, IPsecL3NetworkRefVO ref " +
                                "where ipsec.uuid=ref.connectionUuid and ref.l3NetworkUuid in (:l3Uuids)", IPsecConnectionVO.class)
                        .param("l3Uuids", l3Uuids).list();
                ipsecs.forEach(ipsec -> {
                    ipsec.setStatus(ipsecStatus);
                });
                dbf.updateCollection(ipsecs);
            }
        }
    }

    @Override
    public String preRebootVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeRebootVm(VmInstanceInventory inv) {
        changeIPsecConnStatus(inv, IPSecStatus.Disconnected);
    }

    @Override
    public void afterRebootVm(VmInstanceInventory inv) {

    }

    @Override
    public void failedToRebootVm(VmInstanceInventory inv, ErrorCode reason) {

    }
}

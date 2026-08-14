package org.zstack.network.service.virtualrouter.eip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.eip.EipGlobalConfig;
import org.zstack.network.service.eip.EipVO;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SyncEipRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSyncEipOnStartFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VirtualRouterSyncEipOnStartFlow.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    protected EipConfigProxy proxy;

    @Transactional(readOnly = true)
    private List<EipTO> findEipOnThisRouter(VirtualRouterVmInventory vr, List<String> eipUuids) {
        String sql = "select vip.ip, nic.l3NetworkUuid, nic.ip, vip.l3NetworkUuid from EipVO eip, VipVO vip, VmNicVO nic " +
                " where eip.vipUuid = vip.uuid and vip.serviceProvider in (:providers) "+
                " and eip.vmNicUuid = nic.uuid and eip.uuid in (:euuids)";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("euuids", eipUuids);
        /*just only vrouter vip and skip the flat vip*/
        q.setParameter("providers", Arrays.asList(VyosConstants.PROVIDER_TYPE.toString(), VirtualRouterConstant.PROVIDER_TYPE.toString()));
        List<Tuple> tuples =  q.getResultList();
        List<EipTO> ret = new ArrayList<EipTO>();
        for (Tuple t : tuples) {
            String vipIp = t.get(0, String.class);
            final String l3Uuid = t.get(1, String.class);
            String guestIp = t.get(2, String.class);
            final String pubL3Uuid = t.get(3, String.class);
            VmNicInventory selected = CollectionUtils.findOneOrNull(vr.getVmNics(),
                    arg -> arg.getL3NetworkUuid().equals(l3Uuid));
            String privMac = selected == null ? null : selected.getMac();

            selected = CollectionUtils.findOneOrNull(vr.getVmNics(),
                    arg -> arg.getL3NetworkUuid().equals(pubL3Uuid));
            String publicMac = selected == null ? null : selected.getMac();

            DebugUtils.Assert(privMac!=null, String.format("cannot find private nic[l3NetworkUuid:%s] on virtual router[uuid:%s]",
                    l3Uuid, vr.getUuid()));
            EipTO to = new EipTO();
            to.setVipIp(vipIp);
            to.setPublicMac(publicMac);
            to.setGuestIp(guestIp);
            to.setPrivateMac(privMac);
            to.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
            ret.add(to);
        }

        return ret;
    }

    private List<EipTO> findEipOnThisRouter(final VirtualRouterVmInventory vr, Map<String, Object> data, boolean isNewCreated) throws OperationFailureException {
        List<String> eipUuids = new ArrayList<>();
        if (isNewCreated) {
            final List<VmNicInventory> guestNics = vr.getGuestNics();
            List<String> pubL3Uuids = new ArrayList<>();
            pubL3Uuids.add(vr.getPublicNic().getL3NetworkUuid());
            pubL3Uuids.addAll(vr.getAdditionalPublicNics().stream().map(VmNicInventory::getL3NetworkUuid).collect(Collectors.toList()));
            if (!vr.getPublicNic().getL3NetworkUuid().equals(vr.getManagementNetworkUuid())) {
                /* in old code, eip can be configured on management nic */
                pubL3Uuids.add(vr.getManagementNetworkUuid());
            }

            if (guestNics == null || guestNics.isEmpty()) {
                return new ArrayList<>();
            }

            eipUuids = new Callable<List<String>>() {
                @Override
                @Transactional(readOnly = true)
                public List<String> call() {
                    String sql = "select eip.uuid from EipVO eip, VipVO vip, VmNicVO nic where " +
                            " eip.vipUuid = vip.uuid " +
                            " and eip.vmNicUuid = nic.uuid and vip.l3NetworkUuid in (:vipL3Uuids) and vip.serviceProvider in (:providers) " +
                            " and nic.l3NetworkUuid in (:guestL3Uuid)";
                    TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                    q.setParameter("vipL3Uuids", pubL3Uuids);
                    q.setParameter("guestL3Uuid", guestNics.stream().map(n -> n.getL3NetworkUuid()).collect(Collectors.toList()));
                    /*just only vrouter vip and skip the flat vip*/
                    q.setParameter("providers", Arrays.asList(VyosConstants.PROVIDER_TYPE.toString(), VirtualRouterConstant.PROVIDER_TYPE.toString()));

                    return q.getResultList();
                }
            }.call();

            proxy.attachNetworkService(vr.getUuid(), EipVO.class.getSimpleName(), eipUuids);
            data.put(VirtualRouterSyncEipOnStartFlow.class.getName(), eipUuids);

        } else {
            eipUuids = proxy.getServiceUuidsByRouterUuid(vr.getUuid(), EipVO.class.getSimpleName());
        }

        if (eipUuids.isEmpty()) {
            return new ArrayList<>();
        }

        return findEipOnThisRouter(vr, eipUuids);
    }

    public void run(final FlowTrigger trigger, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        List<VmNicInventory> guestNics = vr.getGuestNics();
        if (guestNics == null || guestNics.isEmpty()) {
            trigger.next();
            return;
        }
        List<String> l3Uuids = guestNics.stream().map(n -> n.getL3NetworkUuid()).collect(Collectors.toList());
        if (!vrMgr.isL3NetworksNeedingNetworkServiceByVirtualRouter(l3Uuids, EipConstant.EIP_NETWORK_SERVICE_TYPE)) {
            trigger.next();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(vr.getUuid()) && !VirtualRouterSystemTags.VR_EIP_ROLE.hasTag(vr.getUuid())) {
            trigger.next();
            return;
        }
        new VirtualRouterRoleManager().makeEipRole(vr.getUuid());

        boolean isNewCreated = data.containsKey(Param.IS_NEW_CREATED.toString());
        List<EipTO> eips;
        try {
            eips = findEipOnThisRouter(vr, data, isNewCreated);
        } catch (OperationFailureException e) {
            trigger.fail(e.getErrorCode());
            return;
        }

        if (eips.isEmpty()) {
            trigger.next();
            return;
        }

        VirtualRouterCommands.SyncEipCmd cmd = new VirtualRouterCommands.SyncEipCmd();
        cmd.setEips(eips);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_SYNC_EIP);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vr.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                SyncEipRsp ret = re.toResponse(SyncEipRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("failed to sync eip on virtual router[uuid:%s], %s",
                            vr.getUuid(), ret.getError());
                    trigger.fail(err);
                } else {
                    String info = String.format("sync eip on virtual router[uuid:%s] successfully",
                            vr.getUuid());
                    logger.debug(info);
                    trigger.next();
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        List<String> eipUuids = (List<String>) data.get(VirtualRouterSyncEipOnStartFlow.class.getName());
        if (eipUuids != null) {
            proxy.detachNetworkService(vr.getUuid(), EipVO.class.getSimpleName(), eipUuids);
        }

        trigger.rollback();
    }
}

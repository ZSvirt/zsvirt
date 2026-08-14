package org.zstack.vpc.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.*;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.OverlayMessage;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.header.vpc.VpcRouterVmVO;
import org.zstack.header.vpc.VpcRouterVmVO_;
import org.zstack.header.vpc.ha.*;
import org.zstack.identity.AccountManager;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.ha.BeforeCleanUpHaGroupNetworkServiceRefsExtensionPoint;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaGroupCleanupExtensionPoint;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.vpc.ha.vpcHaGc.VpcHaGcManager;
import org.zstack.vpc.ha.vpcHaGc.VpcHaGcStruct;
import org.zstack.vpc.ha.vyos.vyosVpcHaRouterBackendManager;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;


public class VpcHaGroupVpcVrImpl implements VirtualRouterHaGroupExtensionPoint, VirtualRouterTrackerExtensionPoint, PreVipReleaseExtensionPoint,
        VirtualRouterHaGroupCleanupExtensionPoint, VirtualRouterHaGetCallbackExtensionPoint {
    private final static CLogger logger = Utils.getLogger(VpcHaGroupVpcVrImpl.class);
    public static ApplianceVmType applianceVmType = new ApplianceVmType(VpcHaGroupConstants.VPCHA_GROUP_VROUTER_VM_TYPE);

    @Autowired
    protected EventFacade evtf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected vyosVpcHaRouterBackendManager vyosHaBackend;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected VpcHaGcManager gcManager;
    @Autowired
    protected VpcHaGroupManager haGroupManager;
    @Autowired
    private VirtualRouterHaBackend haBackend;

    private final String DETACH_L3_NETWORK = "detachL3Network";

    @Override
    public void VirtualRouterVmHaAttachL3Network(String vrUuid, String l3NetworkUuid, boolean applyToVirtualRouter, Completion completion) {
        VirtualRouterVmVO vmVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vmVO.isHaEnabled()) {
            completion.success();
            return;
        }

        String vpcHaGroupUuid = VpcHaGroupOperator.getVpcHaGroupUuid(vrUuid);

        if (!Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, vpcHaGroupUuid)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, l3NetworkUuid)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, L3NetworkVO.class.getSimpleName()).isExists()) {

            VpcHaGroupNetworkServiceRefVO ref = new VpcHaGroupNetworkServiceRefVO();
            ref.setNetworkServiceName(L3NetworkVO.class.getSimpleName());
            ref.setNetworkServiceUuid(l3NetworkUuid);
            ref.setVpcHaRouterUuid(vpcHaGroupUuid);
            dbf.persist(ref);
        }

        /* check vm is one of vpc ha group */
        String peerUuid = VpcHaGroupOperator.getVpcHaRouterPeerUuid(vrUuid);
        if (peerUuid == null){
            completion.success();
            return;
        }

        /* if called from gc */
        if (Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, peerUuid).eq(VmNicVO_.l3NetworkUuid, l3NetworkUuid).isExists()) {
            completion.success();
            return;
        }

        VmAttachNicMsg nicMsg = new VmAttachNicMsg();
        nicMsg.setSystemTags(new ArrayList<>());
        nicMsg.setVmInstanceUuid(peerUuid);
        nicMsg.setL3NetworkUuid(l3NetworkUuid);
        nicMsg.setApplyToBackend(applyToVirtualRouter);
        for (VmNicVO nic: vmVO.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(l3NetworkUuid)) {
                List<String> ips = new ArrayList<>();
                for (UsedIpVO ip : nic.getUsedIps()) {
                    ips.add(ip.getIp());
                }
                nicMsg.getStaticIpMap().put(l3NetworkUuid, ips);
                nicMsg.setDriverType(nic.getDriverType());

            }
        }
        nicMsg.setAllowDuplicatedAddress(true);
        bus.makeTargetServiceIdByResourceUuid(nicMsg, VmInstanceConstant.SERVICE_ID, peerUuid);
        bus.send(nicMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    SQL.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, vpcHaGroupUuid)
                            .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, l3NetworkUuid).delete();
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private void detachL3NetworkFromBackupRouter(String backupVrUuid, String l3NetworkUuid, Completion completion) {
        DetachNicFromVmMsg dmsg = new DetachNicFromVmMsg();
        String nicUuid = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, l3NetworkUuid).eq(VmNicVO_.vmInstanceUuid, backupVrUuid)
                .select(VmNicVO_.uuid).findValue();
        if (nicUuid == null) {
            completion.success();
            return;
        }

        dmsg.setVmNicUuid(nicUuid);
        dmsg.setVmInstanceUuid(backupVrUuid);
        dmsg.setHaPeer(true);
        bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, backupVrUuid);
        bus.send(dmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("vpcHA delete vmnic [uuid:%s] from vr [uuid:%s] failed %s",
                            nicUuid, backupVrUuid, reply.getError().getDetails()));
                }
                completion.success();
            }
        });
    }

    @Override
    public void VirtualRouterVmHaDetachL3Network(String vrUuid, String l3NetworkUuid, boolean isRollback, Completion completion) {
        VirtualRouterVmVO vmVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vmVO.isHaEnabled()) {
            completion.success();
            return;
        }

        SQL.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, l3NetworkUuid)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, L3NetworkVO.class.getSimpleName()).delete();

        /* check vm is one of vpc ha group */
        String peerUuid = VpcHaGroupOperator.getVpcHaRouterPeerUuid(vrUuid);
        if (peerUuid == null){
            completion.success();
            return;
        }

        /* if called from gc */
        if (!Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, l3NetworkUuid).eq(VmNicVO_.vmInstanceUuid, peerUuid).isExists()) {
            completion.success();
            return;
        }

        if (isRollback) {
            VirtualRouterHaTask task = new VirtualRouterHaTask();
            task.setTaskName(DETACH_L3_NETWORK);
            task.setPeerRouterUuid(peerUuid);
            task.setJsonData(l3NetworkUuid);
            submitTaskToHaRouter(task, completion);
        } else {
            detachL3NetworkFromBackupRouter(peerUuid, l3NetworkUuid, completion);
        }
    }

    @Override
    public List<String> getPublicIp(String vrUuid, String l3Uuid) {
        String vpcHaUuid = Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.uuid, vrUuid).select(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid).findValue();
        if (vpcHaUuid == null) {
            return null;
        }

        List<String> vipUuids = Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, vpcHaUuid)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VipVO.class.getSimpleName())
                .select(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid).listValues();
        if (vipUuids == null || vipUuids.isEmpty()) {
            return null;
        }

        List<String> ips = Q.New(VipVO.class).in(VipVO_.uuid, vipUuids).eq(VipVO_.system, true)
                .eq(VipVO_.l3NetworkUuid, l3Uuid).select(VipVO_.ip).listValues();
        if (ips == null || ips.isEmpty()) {
            return null;
        }

        return ips;
    }

    @Override
    public String getPublicIpUuid(String vrUuid, String l3Uuid) {
        String vpcHaUuid = Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.uuid, vrUuid).select(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid).findValue();
        if (vpcHaUuid == null) {
            return null;
        }

        List<String> vipUuids = Q.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, vpcHaUuid)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VipVO.class.getSimpleName())
                .select(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid).listValues();
        if (vipUuids == null || vipUuids.isEmpty()) {
            return null;
        }

        List<String> uuids = Q.New(VipVO.class).in(VipVO_.uuid, vipUuids).eq(VipVO_.system, true)
                .eq(VipVO_.l3NetworkUuid, l3Uuid).select(VipVO_.uuid).listValues();
        if (uuids == null || uuids.isEmpty()) {
            return null;
        }

        return uuids.get(0);
    }

    @Override
    public Boolean isVirtualRouterInSameHaPair(List<String> vrUuids) {
        List<String> haUuids = Q.New(VpcHaGroupApplianceVmRefVO.class).in(VpcHaGroupApplianceVmRefVO_.uuid, vrUuids)
                .select(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid).listValues();
        if (haUuids == null || haUuids.isEmpty()) {
            /* if there is only 1 noha router, return true */
            return vrUuids.size() <= 1;
        }

        haUuids = haUuids.stream().distinct().collect(Collectors.toList());
        return haUuids.size() <= 1;
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
        if (reply.getHaStatus() == null || reply.getHaStatus().equals("")) {
            return;
        }

        if (!Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.uuid, resourceUuid).isExists()) {
            return;
        }

        VpcRouterVmVO vo = Q.New(VpcRouterVmVO.class).eq(VpcRouterVmVO_.uuid, resourceUuid).find();
        if (vo == null) {
            return;
        }

        if (vo.getHaStatus() == null || vo.getHaStatus() == ApplianceVmHaStatus.NoHa) {
            return;
        }

        try {
            ApplianceVmHaStatus newStatus = ApplianceVmHaStatus.valueOf(reply.getHaStatus());
            if (vo.getHaStatus() != newStatus) {
                haGroupManager.setVirtualRouterHaStatus(resourceUuid, newStatus);
            }
            if (newStatus == ApplianceVmHaStatus.Master) {
                String peerUuid = VpcHaGroupOperator.getVpcHaRouterPeerUuid(resourceUuid);
                if (peerUuid == null) {
                    return;
                }

                VpcRouterVmVO peerVo = dbf.findByUuid(peerUuid, VpcRouterVmVO.class);
                if (peerVo == null) {
                    return;
                }
                if (peerVo.getStatus() != ApplianceVmStatus.Connected) {
                    haGroupManager.setVirtualRouterHaStatus(peerUuid, ApplianceVmHaStatus.Backup);
                }
            }
        } catch (Exception e) {
            logger.debug(String.format("got a unknown applianceVm haStatus: %s", reply.getHaStatus()));
        }
    }

    private void syncHa(String vrUuid, Long timeout, Completion completion) {
        VpcRouterVmVO vpc = dbf.findByUuid(vrUuid, VpcRouterVmVO.class);
        if (vpc == null || !vpc.isHaEnabled()) {
            completion.success();
            return;
        }

        vyosHaBackend.enableHa(vrUuid, timeout, completion);
    }

    @Override
    public void syncVirtualRouterHaConfigToBackend(String vrUuid, boolean syncPeer, Completion completion) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("sync-vpcHa-router-%s", vrUuid));
        chain.then(new NoRollbackFlow() {
            String __name__ = "sync-vpcHa-enable-peer-vyosha";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (!syncPeer) {
                    trigger.next();
                    return;
                }

                String peerUuid = VpcHaGroupOperator.getVpcHaRouterPeerUuid(vrUuid);
                if (peerUuid == null) {
                    trigger.next();
                    return;
                }

                /* timeout in 2 mins, it will not block current router */
                syncHa(peerUuid, TimeUnit.MINUTES.toMillis(2), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "sync-vpcHa-enable-vyosha";

            @Override
            public void run(FlowTrigger trigger, Map data) {

                syncHa(vrUuid, null, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion){
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public String getPeerUuid(String vrUuid) {
        return VpcHaGroupOperator.getVpcHaRouterPeerUuid(vrUuid);
    }

    @Override
    public void submitTaskToHaRouter(VirtualRouterHaTask task, Completion completion) {
        VpcHaGcStruct struct = new VpcHaGcStruct();
        struct.setVmInstanceUuid(task.getPeerRouterUuid());
        struct.setTaskName(task.getTaskName());
        struct.setTaskData(task);
        gcManager.submitGc(struct, completion);
    }

    @Override
    public void attachNetworkServiceToHaRouter(String type, List<String> uuids, String vrUuid, boolean override) {
        if (!override) {
            attachNetworkServiceToHaRouter(type, uuids, vrUuid);
            return;
        }

        final boolean vpcExists = dbf.isExist(vrUuid, VpcRouterVmVO.class);
        DebugUtils.Assert(vpcExists, String.format("vpc router[uuid:%s] has been deleted", vrUuid));

        String haGroupUuid = VpcHaGroupOperator.getVpcHaGroupUuid(vrUuid);
        DebugUtils.Assert(haGroupUuid != null, String.format("vpc router[uuid:%s] is not a ha group", vrUuid));

        List<VpcHaGroupNetworkServiceRefVO> oldRefs = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                .eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haGroupUuid)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, type).list();

        List<VpcHaGroupNetworkServiceRefVO> refs = new ArrayList<>();
        for (String uuid : uuids) {
            VpcHaGroupNetworkServiceRefVO ref = new VpcHaGroupNetworkServiceRefVO();
            ref.setVpcHaRouterUuid(haGroupUuid);
            ref.setNetworkServiceName(type);
            ref.setNetworkServiceUuid(uuid);
            refs.add(ref);
        }

        new SQLBatch(){
            @Override
            protected void scripts() {
                for (VpcHaGroupNetworkServiceRefVO ref : oldRefs) {
                    remove(ref);
                }
                for (VpcHaGroupNetworkServiceRefVO ref : refs) {
                    persist(ref);
                }
            }
        }.execute();
    }

    @Override
    public void attachNetworkServiceToHaRouter(String type, List<String> uuids, String vrUuid) {
        VpcRouterVmVO vpcVo = dbf.findByUuid(vrUuid, VpcRouterVmVO.class);
        DebugUtils.Assert(vpcVo != null, String.format("vpc router[uuid:%s] has been deleted", vrUuid));

        String haGroupUuid = VpcHaGroupOperator.getVpcHaGroupUuid(vrUuid);
        DebugUtils.Assert(haGroupUuid != null, String.format("vpc router[uuid:%s] is not a ha group", vrUuid));

        List<VpcHaGroupNetworkServiceRefVO> refs = new ArrayList<>();
        for (String uuid : uuids) {
            if (Q.New(VpcHaGroupNetworkServiceRefVO.class)
                    .eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haGroupUuid)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, type)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, uuid).isExists()) {
                continue;
            }

            VpcHaGroupNetworkServiceRefVO ref = new VpcHaGroupNetworkServiceRefVO();
            ref.setVpcHaRouterUuid(haGroupUuid);
            ref.setNetworkServiceName(type);
            ref.setNetworkServiceUuid(uuid);
            refs.add(ref);
        }

        if (!refs.isEmpty()) {
            dbf.persistCollection(refs);
        }
    }

    @Override
    public void detachNetworkServiceFromHaRouter(String type, List<String> uuids, String vrUuid) {
        if (uuids == null || uuids.isEmpty()) {
            return;
        }

        VpcRouterVmVO vpcVo = dbf.findByUuid(vrUuid, VpcRouterVmVO.class);
        DebugUtils.Assert(vpcVo != null, String.format("vpc router[uuid:%s] has been deleted", vrUuid));

        String vpcHaRouterUuid = VpcHaGroupOperator.getVpcHaGroupUuid(vrUuid);
        DebugUtils.Assert(vpcHaRouterUuid != null, String.format("vpc router[uuid:%s] is not a ha group", vrUuid));

        SQL.New(VpcHaGroupNetworkServiceRefVO.class)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, type)
                .eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, vpcHaRouterUuid)
                .in(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, uuids).delete();
    }

    private List<String> getMasterVpcUuidFromVrUuid(List<String> vpcHaUuids) {
        if (vpcHaUuids == null || vpcHaUuids.isEmpty()) {
            return new ArrayList<>();
        }
        vpcHaUuids = vpcHaUuids.stream().distinct().collect(Collectors.toList());

        List<String> ret = new ArrayList<>();
        for (String hUuid : vpcHaUuids) {
            List<String> vrUuids = Q.New(VpcHaGroupApplianceVmRefVO.class)
                    .eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, hUuid)
                    .select(VpcHaGroupApplianceVmRefVO_.uuid).listValues();

            if (vrUuids == null || vrUuids.isEmpty()) {
                return new ArrayList<>();
            }

            /* return master */
            String masterUuid = null;
            String firstConnectedVpcUuid = null;
            List<VpcRouterVmVO> vrs = Q.New(VpcRouterVmVO.class).in(VpcRouterVmVO_.uuid, vrUuids).list();
            for (VpcRouterVmVO vr : vrs) {
                if (vr.getHaStatus() == ApplianceVmHaStatus.Master) {
                    masterUuid = vr.getUuid();
                    break;
                }

                if (vr.getStatus() == ApplianceVmStatus.Connected) {
                    firstConnectedVpcUuid = vr.getUuid();
                }
            }

            if (masterUuid != null) {
                ret.add(masterUuid);
            }
            else if (firstConnectedVpcUuid != null) {
                ret.add(firstConnectedVpcUuid);
            } else {
                ret.add(vrUuids.get(0));
            }
        }

        return ret;
    }

    @Override
    public List<String> getHaVrUuidsFromNetworkService(String type, String serviceUuid) {
        List<String> vpcHaUuids = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, serviceUuid)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, type)
                .select(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid).listValues();

        return getMasterVpcUuidFromVrUuid(vpcHaUuids);
    }

    @Override
    public List<String> getHaVrUuidsFromNetworkService(String type) {
        List<String> vpcHaUuids = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, type)
                .select(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid).listValues();
        if (vpcHaUuids == null || vpcHaUuids.isEmpty()) {
            return new ArrayList<>();
        }
        vpcHaUuids = vpcHaUuids.stream().distinct().collect(Collectors.toList());

        List<String> ret = new ArrayList<>();
        for (String hUuid : vpcHaUuids) {
            List<String> vrUuids = Q.New(VpcHaGroupApplianceVmRefVO.class)
                    .eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, hUuid)
                    .select(VpcHaGroupApplianceVmRefVO_.uuid).listValues();

            if (vrUuids == null || vrUuids.isEmpty()) {
                continue;
            }

            /* return all vpc */
            ret.addAll(vrUuids);
        }

        return ret;
    }

    @Override
    public List<String> getNetworkServicesFromHaVrUuid(String type, String vrUuid) {
        VpcRouterVmVO vpcVo = dbf.findByUuid(vrUuid, VpcRouterVmVO.class);
        if (vpcVo == null || !vpcVo.isHaEnabled()) {
            return new ArrayList<>();
        }

        String vpcHaRouterUuid = VpcHaGroupOperator.getVpcHaGroupUuid(vrUuid);
        if (vpcHaRouterUuid == null) {
            return new ArrayList<>();
        }

        List<String> uuids = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                .eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, vpcHaRouterUuid)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, type)
                .select(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid)
                .orderBy(VpcHaGroupNetworkServiceRefVO_.id, SimpleQuery.Od.ASC).listValues();

        return uuids;
    }

    @Override
    @Deferred
    public void afterDeleteAllVirtualRouter(VmInstanceInventory vrInv, Completion completion) {
        String vrUuid = vrInv.getUuid();
        String haUuid = Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.uuid, vrUuid)
                .select(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid).findValue();
        if (haUuid == null) {
            logger.debug(String.format("virtual router [uuid:%s] is not ha router", vrUuid));
            completion.success();
            return;
        }

        // 2 vpc ha router maybe delete at same time
        GLock lock = new GLock(String.format("cleanup-ha-group-%s", haUuid), TimeUnit.MINUTES.toSeconds(30));
        lock.lock();
        Defer.defer(lock::unlock);

        /* more than 1 router existed */
        if (Q.New(VpcHaGroupApplianceVmRefVO.class).eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, haUuid).count() > 1) {
            logger.debug(String.format("virtual router [uuid:%s] is not last router", vrUuid));
            completion.success();
            return;
        }

        for (BeforeCleanUpHaGroupNetworkServiceRefsExtensionPoint ext : pluginRgty.getExtensionList(BeforeCleanUpHaGroupNetworkServiceRefsExtensionPoint.class)) {
            ext.beforeCleanUp(vrInv);
        }

        /* delete system vip */
        List<String> vips = new VpcHaGroupOperator().getVpcHaGroupSystemVipUuids(haUuid);
        if (vips == null || vips.isEmpty()) {
            logger.debug(String.format("virtual router [uuid:%s] does no have system vip", vrUuid));
            completion.success();
            return;
        }

        new While<>(vips).step((vip, compl) -> {
            VipDeletionMsg rmsg = new VipDeletionMsg();
            rmsg.setVipUuid(vip);
            bus.makeTargetServiceIdByResourceUuid(rmsg, VipConstant.SERVICE_ID, vip);
            bus.send(rmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    compl.done();
                }
            });
        }, 10).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                SQL.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haUuid).delete();
                completion.success();
            }
        });
    }

    @Override
    public List<String> getNetworkServicesFromHaGroupUuid(String type, String haGroupUuid) {
        return Q.New(VpcHaGroupNetworkServiceRefVO.class).select(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, type)
                .eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, haGroupUuid).listValues();
    }

    @Override
    public void preReleaseServicesOnVip(VipInventory vip, Completion completion) {
        /* for some concurrent error: there is no service attached to vip, but vip is still attached to virtual router */
        SQL.New(VpcHaGroupNetworkServiceRefVO.class).eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VipVO.class.getSimpleName())
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, vip.getUuid()).delete();
        completion.success();
    }

    @Override
    public String getHaGroupName(String vrUuid) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (vrVo == null) {
            return null;
        }

        if (!vrVo.isHaEnabled()) {
            return null;
        }

        String haUuid = VpcHaGroupOperator.getVpcHaGroupUuid(vrUuid);
        if (haUuid == null) {
            return null;
        }

        return Q.New(VpcHaGroupVO.class).select(VpcHaGroupVO_.name).eq(VpcHaGroupVO_.uuid, haUuid).findValue();
    }

    @Override
    public String getHaGroupUuid(String vrUuid) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (vrVo == null) {
            return null;
        }

        if (!vrVo.isHaEnabled()) {
            return null;
        }

        return VpcHaGroupOperator.getVpcHaGroupUuid(vrUuid);
    }

    @Override
    public void virtualRouterOverlayMsgHandle(OverlayMessage msg, Completion completion) {
        if (msg instanceof VirtualRouterOverlayMsg) {
            String haUuid = haBackend.getVirtualRouterHaUuid(((VirtualRouterOverlayMsg) msg).getVmInstanceUuid());
            if (haUuid == null) {
                completion.fail(operr("ha group uuid nil"));
                return;
            }

            bus.makeTargetServiceIdByResourceUuid(msg, VpcHaGroupConstants.SERVICE_ID, haUuid);
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply r) {
                    if (!r.isSuccess()) {
                        completion.fail(r.getError());
                    } else {
                        completion.success();
                    }
                }
            });
        }
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct detachL3Network = new VirtualRouterHaCallbackStruct();
        detachL3Network.type = DETACH_L3_NETWORK;
        detachL3Network.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                String backVrUuid = task.getPeerRouterUuid();
                String l3NetworkUuid = task.getJsonData();
                detachL3NetworkFromBackupRouter(backVrUuid, l3NetworkUuid, completion);
            }
        };
        structs.add(detachL3Network);

        return structs;
    }
}

package org.zstack.storage.primary.sharedblock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.ha.CancelStorageHeartbeatFileAndGatewayPingOnHostMsg;
import org.zstack.ha.HaConstants;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Create by weiwang at 2018/4/8
 */
public class SharedBlockKvmFactory implements SharedBlockHypervisorFactory, KVMHostConnectExtensionPoint,
        KVMPingAgentNoFailureExtensionPoint, HostChangeStateExtensionPoint, HostAfterMaintenanceExtensionPoint,
        KVMStartVmExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SharedBlockKvmFactory.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public SharedBlockHypervisorBackend getHypervisorBackend(PrimaryStorageVO vo) {
        return new SharedBlockKvmBackend(vo);
    }

    @Transactional(readOnly = true)
    private List<SharedBlockGroupPrimaryStorageInventory> findSharedBlockByHostUuid(String clusterUuid) {
        List<SharedBlockGroupVO> ret = SQL.New("select pri from SharedBlockGroupVO pri, PrimaryStorageClusterRefVO ref" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = :cuuid", SharedBlockGroupVO.class)
                .param("cuuid", clusterUuid)
                .list();
        return ret.isEmpty() ? null : SharedBlockGroupPrimaryStorageInventory.valueOf1(ret);
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __init__ = "init-shared-block-primary-storage";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<SharedBlockGroupPrimaryStorageInventory> pss = findSharedBlockByHostUuid(context.getInventory().getClusterUuid());
                if (pss == null || pss.isEmpty()) {
                    trigger.next();
                    return;
                }

                ErrorCodeList errList = (ErrorCodeList) data.get(KVMConstant.CONNECT_HOST_PRIMARYSTORAGE_ERROR);
                new While<>(pss).each((ps, whileCompletion) -> {
                    InitKvmHostMsg msg = new InitKvmHostMsg();
                    msg.setHypervisorType(context.getInventory().getHypervisorType());
                    msg.setHostUuid(context.getInventory().getUuid());
                    msg.setPrimaryStorageUuid(ps.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps.getUuid());
                    bus.send(msg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("fail to connect shared block[uuid:%s] from host[uuid:%s], because:%s"
                                        , ps.getUuid(), context.getInventory().getUuid(), reply.getError().toString()));
                                errList.getCauses().add(reply.getError());
                                whileCompletion.done();
                                return;
                            }

                            InitKvmHostReply reply1 = reply.castReply();
                            if (!reply1.isSuccess()) {
                                logger.warn(String.format("fail to connect shared block[uuid:%s] from host[uuid:%s], because:%s"
                                        , ps.getUuid(), context.getInventory().getUuid(), reply1.getError().toString()));
                                errList.getCauses().add(reply1.getError());
                                whileCompletion.done();
                                return;
                            }

                            RecalculatePrimaryStorageCapacityMsg msg = new RecalculatePrimaryStorageCapacityMsg();
                            msg.setPrimaryStorageUuid(ps.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps.getUuid());
                            bus.send(msg);
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        data.put(KVMConstant.CONNECT_HOST_PRIMARYSTORAGE_ERROR, errList);
                        trigger.next();
                    }
                });
            }
        };
    }

    private void pingHostAttachedPrimaryStorages(KVMHostInventory host) {
        List<String> connectedPrimaryStorages = getSharedBlockGroupPrimaryStorageConnectedOnHost(host);
        if (connectedPrimaryStorages.isEmpty()) {
            return;
        }
        PingHostAttachedPrimaryStoragesMsg pmsg = new PingHostAttachedPrimaryStoragesMsg();
        pmsg.setPrimaryStorageUuids(connectedPrimaryStorages);
        pmsg.setHostUuid(host.getUuid());
        pmsg.setHypervisorType(host.getHypervisorType());
        bus.makeTargetServiceIdByResourceUuid(pmsg, PrimaryStorageConstant.SERVICE_ID, host.getUuid());
        bus.send(pmsg);
    }

    @Override
    public void kvmPingAgentNoFailure(KVMHostInventory host, NoErrorCompletion completion) {
        Set<String> disconnectedPrimaryStorages = checkSharedBlockGroupPrimaryStorageDisconnectedOnHost(host);

        new While<>(disconnectedPrimaryStorages).each((psUuid, whileCompletion) -> {
            logger.debug(String.format("init primary storage %s on host %s since storage host status is %s",
                    psUuid, host.getUuid(), PrimaryStorageHostStatus.Disconnected.toString()));
            InitKvmHostMsg msg = new InitKvmHostMsg();
            msg.setExpectStatus(PrimaryStorageHostStatus.Disconnected);
            msg.setHypervisorType(getHypervisorType());
            msg.setHostUuid(host.getUuid());
            msg.setPrimaryStorageUuid(psUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid);
            bus.send(msg);
            whileCompletion.done();
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                pingHostAttachedPrimaryStorages(host);
                completion.done();
            }
        });
    }

    public static Set<String> checkSharedBlockGroupPrimaryStorageDisconnectedOnHost(KVMHostInventory host) {
        if (!host.getStatus().equals(HostStatus.Connected.toString())) {
            //NOTE(weiw): no need to reconnect ps on host, since host is reconnecting or will be reconnecting
            return Collections.emptySet();
        }
        List<String> psUuids = SQL.New("select hr.primaryStorageUuid from SharedBlockGroupPrimaryStorageHostRefVO hr, PrimaryStorageClusterRefVO cr " +
                "where cr.primaryStorageUuid = hr.primaryStorageUuid " +
                "and cr.clusterUuid = :cuuid " +
                "and hr.hostUuid = :huuid " +
                "and hr.status = :status")
                .param("cuuid", host.getClusterUuid())
                .param("huuid", host.getUuid())
                .param("status", PrimaryStorageHostStatus.Disconnected)
                .list();
        return new HashSet<>(psUuids);
    }

    public static List<String> getSharedBlockGroupPrimaryStorageConnectedOnHost(KVMHostInventory host) {
        if (!host.getStatus().equals(HostStatus.Connected.toString())) {
            return new ArrayList<>();
        }

        List<String> psUuids = SQL.New("select hr.primaryStorageUuid from SharedBlockGroupPrimaryStorageHostRefVO hr, PrimaryStorageClusterRefVO cr " +
                        "where cr.primaryStorageUuid = hr.primaryStorageUuid " +
                        "and cr.clusterUuid = :cuuid " +
                        "and hr.hostUuid = :huuid " +
                        "and hr.status = :status")
                .param("cuuid", host.getClusterUuid())
                .param("huuid", host.getUuid())
                .param("status", PrimaryStorageHostStatus.Connected)
                .list();
        return psUuids.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public void preChangeHostState(HostInventory inventory, HostStateEvent event, HostState nextState) throws HostException {
    }

    @Override
    public void beforeChangeHostState(HostInventory inventory, HostStateEvent event, HostState nextState) {

    }

    @Override
    public void afterChangeHostState(HostInventory inventory, HostStateEvent event, HostState previousState) {
    }

    @Override
    public void afterMaintenanceExtensionPoint(HostInventory hostInventory, Completion completion) {
        List<SharedBlockGroupPrimaryStorageInventory> sharedBlockGroups = findSharedBlockByHostUuid(hostInventory.getClusterUuid());
        if (sharedBlockGroups == null || sharedBlockGroups.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(sharedBlockGroups).each((storageInventory, whileCompletion) -> {
            DisconnectSharedBlockGroupPrimaryStorageOnHostMsg dmsg = new DisconnectSharedBlockGroupPrimaryStorageOnHostMsg();
            dmsg.setHostUuid(hostInventory.getUuid());
            dmsg.setHypervisorType(hostInventory.getHypervisorType());
            dmsg.setPrimaryStorageUuid(storageInventory.getUuid());
            dmsg.setStopService(true);
            bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, storageInventory.getUuid());
            bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    CancelStorageHeartbeatFileAndGatewayPingOnHostMsg cmsg = new CancelStorageHeartbeatFileAndGatewayPingOnHostMsg();
                    cmsg.setPrimaryStorageInventory(storageInventory);
                    cmsg.setHostInventory(hostInventory);
                    bus.makeTargetServiceIdByResourceUuid(cmsg, HaConstants.SERVICE_ID, cmsg.getPrimaryStorageInventory().getUuid());
                    bus.send(cmsg);
                    //NOTE(weiw): not block maintenance process if disconnect failed
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (spec.getMemorySnapshotUuid() == null) {
            return;
        }

        VolumeSnapshotVO vo = dbf.findByUuid(spec.getMemorySnapshotUuid(), VolumeSnapshotVO.class);

        if (!Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.type, SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE)
                .eq(PrimaryStorageVO_.uuid, vo.getPrimaryStorageUuid()).isExists()) {
            return;
        }

        cmd.setMemorySnapshotPath(vo.getPrimaryStorageInstallPath());
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }
}

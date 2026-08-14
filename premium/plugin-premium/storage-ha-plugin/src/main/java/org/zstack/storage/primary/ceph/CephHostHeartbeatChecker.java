package org.zstack.storage.primary.ceph;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.SQL;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.ha.*;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephSystemTags;
import org.zstack.storage.ceph.primary.*;
import org.zstack.storage.primary.HaStoreFindHostUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class CephHostHeartbeatChecker implements HaHostStorageBasedChecker, CephPrimaryStorageMonAfterModifiedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(CephHostHeartbeatChecker.class);
    public static final String CEPH_HOST_HEARTBEAT_CHECK_PATH = "/ceph/host/heartbeat/check";

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;

    public static class CheckHostHeartbeatCmd extends KVMAgentCommands.AgentCommand {
        public long interval;
        public long times;
        public List<String> poolNames;
        public String primaryStorageUuid;
        public String targetHostUuid;
        public int storageCheckerTimeout;
        public String manufacturer;
    }

    public static class CheckHostHeartbeatRsp extends KVMAgentCommands.AgentResponse {
        public HashMap<String, Boolean> result;
        public List<String> vmUuids;
    }

    public void check(CheckerStruct struct, HaCheckerCompletion completion) {
        List<String> failureHostRelatedPrimaryStorageUuids = Q.New(PrimaryStorageHostRefVO.class)
                .select(PrimaryStorageHostRefVO_.primaryStorageUuid)
                .eq(PrimaryStorageHostRefVO_.hostUuid, struct.getHostUuid())
                .listValues();

        if (failureHostRelatedPrimaryStorageUuids.isEmpty()) {
            logger.debug(String.format("no related storage of host[uuid: %s] can be found",
                    struct.getHostUuid()));
            completion.noWay();
            return;
        }

        failureHostRelatedPrimaryStorageUuids = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.uuid)
                .eq(PrimaryStorageVO_.type, CephConstants.CEPH_PRIMARY_STORAGE_TYPE)
                .in(PrimaryStorageVO_.uuid, failureHostRelatedPrimaryStorageUuids)
                .listValues();

        if (failureHostRelatedPrimaryStorageUuids.isEmpty()) {
            logger.debug(String.format("no ceph storage related to the failure host[uuid: %s] can be found",
                    struct.getHostUuid()));
            completion.noWay();
            return;
        }

        List<String> currentVmUsedPrimaryStorageUuids = struct.getVmInstance()
                .getAllDiskVolumes()
                .stream()
                .map(VolumeInventory::getPrimaryStorageUuid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // get primary storage uuid whose connectivity need to be checked
        currentVmUsedPrimaryStorageUuids.retainAll(failureHostRelatedPrimaryStorageUuids);
        if (currentVmUsedPrimaryStorageUuids.isEmpty()) {
            logger.debug(String.format("vm[uuid: %s] do not have any storage connected to current failure host's cluster",
                    struct.getVmInstance().getUuid()));
            completion.noWay();
            return;
        }

        // because current host (included in struct is not reachable)
        // find sibling host to execute host-storage heartbeat check
        final List<String> siblings = findSiblingHosts(struct, currentVmUsedPrimaryStorageUuids);
        if (siblings.isEmpty()) {
            logger.debug(String.format("no connected host in current cluster[uuid: %s] with host[uuid: %s]",
                    struct.getVmInstance().getClusterUuid(), struct.getHostUuid()));
            completion.noWay();
            return;
        }

        Collections.shuffle(siblings);
        Collections.shuffle(currentVmUsedPrimaryStorageUuids);

        doCephHostHeartbeatCheck(struct.getHostUuid(), siblings.get(0), currentVmUsedPrimaryStorageUuids.get(0), completion);
    }

    private List<String> findSiblingHosts(CheckerStruct struct, List<String> currentVmUsedCephStorageUuids) {
        List<String> candidates = HaStoreFindHostUtils.findSiblingsFromSameCluster(struct.getHostUuid(), struct.getVmInstance().getClusterUuid());
        if (candidates == null || candidates.isEmpty()) {
            return HaStoreFindHostUtils.findSiblingFromOtherCluster(struct.getVmInstance().getClusterUuid(), currentVmUsedCephStorageUuids);
        } else {
            return candidates;
        }
    }

    public int getWeight() {
        return 5;
    }

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return PrimaryStorageType.valueOf(CephConstants.CEPH_PRIMARY_STORAGE_TYPE);
    }

    /**
     *
     * @param targetHostUuid disconnected host uuid
     * @param peerHostUuid connected peer host to execute heartbeat check
     * @param primaryStorageUuid uuid where vm volumes is located and connected to current host
     * @param completion
     */
    private void doCephHostHeartbeatCheck(String targetHostUuid, String peerHostUuid, String primaryStorageUuid, HaCheckerCompletion completion) {
        CheckHostHeartbeatCmd cmd = new CheckHostHeartbeatCmd();
        cmd.interval = HaGlobalConfig.HOST_SELF_FENCER_INTERVAL.value(Long.class);
        cmd.times = HaGlobalConfig.HOST_CHECK_MAX_ATTEMPTS.value(Long.class);;
        cmd.primaryStorageUuid = primaryStorageUuid;
        cmd.poolNames = Q.New(CephPrimaryStoragePoolVO.class)
                .select(CephPrimaryStoragePoolVO_.poolName)
                .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, primaryStorageUuid)
                .eq(CephPrimaryStoragePoolVO_.type, CephPrimaryStoragePoolType.Root.toString())
                .listValues();
        cmd.targetHostUuid = targetHostUuid;
        cmd.storageCheckerTimeout = HaGlobalConfig.STORAGE_CHECKER_TIMEOUT.value(Integer.class);
        if (CephSystemTags.CEPH_MANUFACTURER.hasTag(primaryStorageUuid)) {
            cmd.manufacturer = CephSystemTags.CEPH_MANUFACTURER.getTokenByResourceUuid(primaryStorageUuid, CephSystemTags.CEPH_MANUFACTURER_TOKEN);
        }

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(peerHostUuid);
        msg.setPath(CEPH_HOST_HEARTBEAT_CHECK_PATH);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());

        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.debug(String.format("failed to send message check heartbeat of failure host[uuid: %s] on host[uuid: %s]",
                            targetHostUuid, peerHostUuid));
                    completion.notStable();
                    return;
                }

                KVMHostAsyncHttpCallReply kr = reply.castReply();
                CheckHostHeartbeatRsp rsp = kr.toResponse(CheckHostHeartbeatRsp.class);
                if (!rsp.isSuccess()) {
                    logger.debug(String.format("exception occurred when agent check heartbeat of failure host[uuid: %s] on host[uuid: %s]",
                            targetHostUuid, peerHostUuid));
                    completion.notStable();
                    return;
                }

                if (rsp.result == null || rsp.result.isEmpty()) {
                    logger.debug(String.format("response do not contain any result of check heartbeat of failure host[uuid: %s] on host[uuid: %s]",
                            targetHostUuid, peerHostUuid));
                    completion.notStable();
                    return;
                }

                Collection<Boolean> res = rsp.result.values();
                if (res.contains(Boolean.TRUE)) {
                    completion.success(rsp.vmUuids);
                } else {
                    completion.fail(operr("host %s's heartbeat is not updated", targetHostUuid));
                }
            }
        });
    }

    @Override
    public void afterModified(CephPrimaryStorageVO vo) {
        if (!HaGlobalConfig.ALL.value(Boolean.class)) {
            return;
        }

        // find hosts conenct to this ceph storage
        List<String> clusterUuids = Q.New(PrimaryStorageClusterRefVO.class)
                .select(PrimaryStorageClusterRefVO_.clusterUuid)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, vo.getUuid())
                .listValues();
        if (clusterUuids.isEmpty()) {
            logger.debug("primary storage[uuid:%s] is not attached to any cluster, no need to refresh fencer");
            return;
        }

        List<String> hostUuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .in(HostVO_.clusterUuid, clusterUuids)
                .listValues();
        PrimaryStorageInventory primaryStorage = PrimaryStorageInventory.valueOf(vo);

        // queue cancel self fencer operation
        // because multi changes of mon configuration only need to cancel once
        // if there is a cancel operation detected, cancelFencerQueued flag will be
        // set to true which means there is a cancel operation in progress skip it.
        // queue setup operation in the same queue so that if new cancel fencer operation
        // is requested, it will be executed util last setup fencer operation finished
        AtomicBoolean cancelFencerQueued = new AtomicBoolean(false);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("refresh-ceph-primary-storage-fencer-flow");
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("cancel-self-fencer-of-ceph-%s", vo.getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                thdf.chainSubmit(new ChainTask(trigger) {
                    @Override
                    public String getSyncSignature() {
                        return String.format("refresh-ceph-primary-storage-%s-fencer", vo.getUuid());
                    }

                    @Override
                    public void run(SyncTaskChain chain) {
                        doCancelCephPrimaryStorageSelfFencer(primaryStorage, hostUuids, new NoErrorCompletion(trigger, chain) {
                            @Override
                            public void done() {
                                trigger.next();
                                chain.next();
                            }
                        });
                    }

                    @Override
                    public String getName() {
                        return getSyncSignature();
                    }

                    protected void exceedMaxPendingCallback() {
                        cancelFencerQueued.getAndSet(true);
                        trigger.next();
                    }

                    protected int getMaxPendingTasks() {
                        return 0;
                    }

                    protected String getDeduplicateString() {
                        return String.format("cancel-ceph-primary-storage-%s-fencer", vo.getUuid());
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("setup-self-fencer-of-ceph-%s", vo.getUuid());

            @Override
            public boolean skip(Map data) {
                return cancelFencerQueued.get();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                thdf.chainSubmit(new ChainTask(trigger) {
                    @Override
                    public String getSyncSignature() {
                        return String.format("refresh-ceph-primary-storage-%s-fencer", vo.getUuid());
                    }

                    @Override
                    public void run(SyncTaskChain chain) {
                        doSetupCephPrimaryStorageSelfFencer(primaryStorage, hostUuids, new NoErrorCompletion(trigger, chain) {
                            @Override
                            public void done() {
                                trigger.next();
                                chain.next();
                            }
                        });
                    }

                    @Override
                    public String getName() {
                        return getSyncSignature();
                    }
                });
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errCode, Map data) {

            }
        }).done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {

            }
        }).start();
    }



    private void doSetupCephPrimaryStorageSelfFencer(PrimaryStorageInventory primaryStorage, List<String> hostUuids, NoErrorCompletion completion) {
        List<SetupSelfFencerOnKvmHostMsg> smsgs = new ArrayList<>();
        for (String hostUuid : hostUuids) {
            KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam param = new KvmSetupSelfFencerExtensionPoint.KvmSetupSelfFencerParam();
            param.setHostUuid(hostUuid);
            param.setInterval(HaGlobalConfig.HOST_SELF_FENCER_INTERVAL.value(Long.class));
            param.setMaxAttempts(HaGlobalConfig.HOST_SELF_FENCER_ATTEMPTS.value(Integer.class));
            param.setStorageCheckerTimeout(HaGlobalConfig.STORAGE_CHECKER_TIMEOUT.value(Integer.class));
            param.setPrimaryStorage(primaryStorage);
            param.setStrategy(HaStrategyHelper.getPrimaryStorageSelfFencerStrategy(primaryStorage.getUuid(), primaryStorage.getType()).toString());
            param.setFencers(HaHelper.findExecuteFencers());
            SetupSelfFencerOnKvmHostMsg smsg = new SetupSelfFencerOnKvmHostMsg();
            smsg.setParam(param);
            bus.makeTargetServiceIdByResourceUuid(smsg, PrimaryStorageConstant.SERVICE_ID, param.getPrimaryStorage().getUuid());
            smsgs.add(smsg);
        }

        new While<>(smsgs).each((cmsg, whileCompletion) -> {
            bus.send(cmsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.debug(String.format("failed to setup fencer of primary storage[uuid: %s] on host[uuid: %s]",
                                cmsg.getParam().getPrimaryStorage().getUuid(), cmsg.getParam().getHostUuid()));
                    }

                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    private void doCancelCephPrimaryStorageSelfFencer(PrimaryStorageInventory primaryStorage, List<String> hostUuids, NoErrorCompletion completion) {
        List<CancelSelfFencerOnKvmHostMsg> cmsgs = new ArrayList<>();
        for (String hostUuid : hostUuids) {
            KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam param = new KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam();
            param.setHostUuid(hostUuid);
            param.setPrimaryStorage(primaryStorage);
            CancelSelfFencerOnKvmHostMsg cmsg = new CancelSelfFencerOnKvmHostMsg();
            cmsg.setParam(param);
            bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, param.getPrimaryStorage().getUuid());
            cmsgs.add(cmsg);
        }

        new While<>(cmsgs).each((cmsg, whileCompletion) -> {
            bus.send(cmsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.debug(String.format("failed to cancel fencer of primary storage[uuid: %s] on host[uuid: %s]",
                                cmsg.getParam().getPrimaryStorage().getUuid(), cmsg.getParam().getHostUuid()));
                    }

                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }
}

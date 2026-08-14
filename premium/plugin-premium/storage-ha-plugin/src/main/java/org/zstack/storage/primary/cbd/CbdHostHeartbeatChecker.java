package org.zstack.storage.primary.cbd;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.cbd.kvm.KvmCbdCommands;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.ha.CheckerStruct;
import org.zstack.ha.HaGlobalConfig;
import org.zstack.ha.HaHostStorageBasedChecker;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;
import org.zstack.storage.primary.HaStoreFindHostUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class CbdHostHeartbeatChecker implements HaHostStorageBasedChecker {
    private static final CLogger logger = Utils.getLogger(CbdHostHeartbeatChecker.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ExternalPrimaryStorageFactory extPsFactory;

    public static class CheckHostHeartbeatCmd extends KVMAgentCommands.AgentCommand {
        public long interval;
        public long times;
        public String primaryStorageUuid;
        public String hostUuid;
        public String heartbeatUrl;
        public Integer hostId;
        public int storageCheckerTimeout;
    }

    public static class CheckHostHeartbeatRsp extends KVMAgentCommands.AgentResponse {
        public HashMap<String, Boolean> result;
        public List<String> vmUuids;
    }

    @Override
    public void check(CheckerStruct struct, HaCheckerCompletion completion) {
        List<String> failureHostRelatedPrimaryStorageUuids = Q.New(PrimaryStorageClusterRefVO.class)
                .select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                .eq(PrimaryStorageClusterRefVO_.clusterUuid, struct.getVmInstance().getClusterUuid())
                .listValues();

        if (failureHostRelatedPrimaryStorageUuids.isEmpty()) {
            logger.debug(String.format("no related storage of host[uuid: %s] can be found",
                    struct.getHostUuid()));
            completion.noWay();
            return;
        }

        failureHostRelatedPrimaryStorageUuids = Q.New(ExternalPrimaryStorageVO.class)
                .select(PrimaryStorageVO_.uuid)
                .eq(PrimaryStorageVO_.type, PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE)
                .eq(ExternalPrimaryStorageVO_.defaultProtocol, VolumeProtocol.CBD.name())
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
            completion.notStable();
            return;
        }

        Collections.shuffle(siblings);
        Collections.shuffle(currentVmUsedPrimaryStorageUuids);

        doHostHeartbeatCheck(struct.getHostUuid(), siblings.get(0), currentVmUsedPrimaryStorageUuids.get(0), completion);
    }

    private List<String> findSiblingHosts(CheckerStruct struct, List<String> currentVmUsedStorageUuids) {
        List<String> candidates = HaStoreFindHostUtils.findSiblingsFromSameCluster(struct.getHostUuid(), struct.getVmInstance().getClusterUuid());
        if (candidates == null || candidates.isEmpty()) {
            return HaStoreFindHostUtils.findSiblingFromOtherCluster(struct.getVmInstance().getClusterUuid(), currentVmUsedStorageUuids);
        } else {
            return candidates;
        }
    }

    public int getWeight() {
        return 5;
    }

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return PrimaryStorageType.valueOf(PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE);
    }

    /**
     *
     * @param targetHostUuid disconnected host uuid
     * @param peerHostUuid connected peer host to execute heartbeat check
     * @param primaryStorageUuid uuid where vm volumes is located and connected to current host
     * @param completion
     */
    private void doHostHeartbeatCheck(String targetHostUuid, String peerHostUuid, String primaryStorageUuid, HaCheckerCompletion<List<String>> completion) {
        HostInventory targetHost = HostInventory.valueOf(dbf.findByUuid(targetHostUuid, HostVO.class));
        // FIXME: assume every cbd heartbeat url is same
        HeartbeatVolumeTO targetHbVol = extPsFactory.getNodeSvc(primaryStorageUuid).getHeartbeatVolumeActiveInfo(targetHost);
        Integer hostId = Q.New(ExternalPrimaryStorageHostRefVO.class).select(ExternalPrimaryStorageHostRefVO_.hostId)
                .eq(ExternalPrimaryStorageHostRefVO_.hostUuid, targetHostUuid)
                .eq(ExternalPrimaryStorageHostRefVO_.primaryStorageUuid, primaryStorageUuid)
                .findValue();

        if (hostId == null) {
            completion.fail(operr("not found hostId for hostUuid[%s] and primaryStorageUuid[%s]", targetHostUuid, primaryStorageUuid));
            return;
        }


        CheckHostHeartbeatCmd cmd = new CheckHostHeartbeatCmd();
        cmd.interval = HaGlobalConfig.HOST_SELF_FENCER_INTERVAL.value(Long.class);
        cmd.times = HaGlobalConfig.HOST_CHECK_MAX_ATTEMPTS.value(Long.class);
        cmd.primaryStorageUuid = primaryStorageUuid;
        cmd.storageCheckerTimeout = HaGlobalConfig.STORAGE_CHECKER_TIMEOUT.value(Integer.class);
        cmd.heartbeatUrl = targetHbVol.getInstallPath();
        cmd.hostId = hostId;
        cmd.hostUuid = targetHostUuid;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(peerHostUuid);
        msg.setPath(KvmCbdCommands.CBD_CHECK_VMSTATE_PATH);
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
}

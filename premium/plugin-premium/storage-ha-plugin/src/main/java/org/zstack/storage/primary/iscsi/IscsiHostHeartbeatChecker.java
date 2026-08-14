package org.zstack.storage.primary.iscsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.ha.CheckerStruct;
import org.zstack.ha.HaGlobalConfig;
import org.zstack.ha.HaHostChecker;
import org.zstack.ha.HaHostStorageBasedChecker;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_;
import org.zstack.header.storage.addon.primary.HeartbeatVolumeTO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.iscsi.kvm.KvmIscsiCommands;
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

public class IscsiHostHeartbeatChecker implements HaHostStorageBasedChecker {
    private static final CLogger logger = Utils.getLogger(IscsiHostHeartbeatChecker.class);

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

        // use
        failureHostRelatedPrimaryStorageUuids = Q.New(ExternalPrimaryStorageVO.class)
                .select(PrimaryStorageVO_.uuid)
                .eq(PrimaryStorageVO_.type, PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE)
                // FIXME: hard code here, use PrimaryStorageOutputProtocolRefVO to get output protocol
                .in(ExternalPrimaryStorageVO_.defaultProtocol, Arrays.asList(VolumeProtocol.Vhost.name(), VolumeProtocol.iSCSI.name()))
                .in(PrimaryStorageVO_.uuid, failureHostRelatedPrimaryStorageUuids)
                .listValues();

        if (failureHostRelatedPrimaryStorageUuids.isEmpty()) {
            logger.debug(String.format("no iscsi storage related to the failure host[uuid: %s] can be found",
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

        doIscsiHostHeartbeatCheck(struct.getHostUuid(), siblings.get(0), currentVmUsedPrimaryStorageUuids.get(0), completion);
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
    private void doIscsiHostHeartbeatCheck(String targetHostUuid, String peerHostUuid, String primaryStorageUuid, HaCheckerCompletion<List<String>> completion) {
        HostInventory targetHost = HostInventory.valueOf(dbf.findByUuid(targetHostUuid, HostVO.class));
        // FIXME: assume every iscsi heartbeat url is same
        HeartbeatVolumeTO targetHbVol = extPsFactory.getNodeSvc(primaryStorageUuid).getHeartbeatVolumeActiveInfo(targetHost);


        CheckHostHeartbeatCmd cmd = new CheckHostHeartbeatCmd();
        cmd.interval = HaGlobalConfig.HOST_SELF_FENCER_INTERVAL.value(Long.class);
        cmd.times = HaGlobalConfig.HOST_CHECK_MAX_ATTEMPTS.value(Long.class);;
        cmd.primaryStorageUuid = primaryStorageUuid;
        cmd.storageCheckerTimeout = HaGlobalConfig.STORAGE_CHECKER_TIMEOUT.value(Integer.class);
        cmd.heartbeatUrl = targetHbVol.getInstallPath();
        cmd.hostId = targetHbVol.getHostId();
        cmd.hostUuid = targetHostUuid;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(peerHostUuid);
        msg.setPath(KvmIscsiCommands.ISCSI_CHECK_VMSTATE_PATH);
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

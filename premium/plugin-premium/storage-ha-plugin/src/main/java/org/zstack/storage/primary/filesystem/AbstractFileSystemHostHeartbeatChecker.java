package org.zstack.storage.primary.filesystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.ha.CheckerStruct;
import org.zstack.ha.HaGlobalConfig;
import org.zstack.ha.HaHostChecker;
import org.zstack.ha.HaHostStorageBasedChecker;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.primary.HaStoreFindHostUtils;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * @Author: DaoDao
 * @Date: 2023/3/8
 */
public abstract class AbstractFileSystemHostHeartbeatChecker implements HaHostStorageBasedChecker {
    private static final CLogger logger = Utils.getLogger(AbstractFileSystemHostHeartbeatChecker.class);
    public static final String FILESYSTEM_CHECK_VMSTATE_PATH = "/filesystem/check/vmstate";

    @Autowired
    private CloudBus bus;

    public static class CheckFileSystemVmStateCmd extends KVMAgentCommands.AgentCommand {
        public long interval;
        public long times;
        public String primaryStorageUuid;
        public String targetHostUuid;
        public int storageCheckerTimeout;
        public String mountPath;
    }

    public static class CheckFileSystemVmStateRsp extends KVMAgentCommands.AgentResponse {
        public HashMap<String, Boolean> result;
        public List<String> vmUuids;
    }


    protected List<String> findSiblingHosts(CheckerStruct struct, List<String> currentVmUsedFileSystemStorageUuids) {
        List<String> candidates = HaStoreFindHostUtils.findSiblingsFromSameCluster(struct.getHostUuid(), struct.getVmInstance().getClusterUuid());
        if (candidates == null || candidates.isEmpty()) {
            return HaStoreFindHostUtils.findSiblingFromOtherCluster(struct.getVmInstance().getClusterUuid(), currentVmUsedFileSystemStorageUuids);
        } else {
            return candidates;
        }
    }

    protected List<String> findCurrentVmUsedPrimaryStorageUuids(CheckerStruct struct) {
        List<String> failureHostRelatedPrimaryStorageUuids = Q.New(PrimaryStorageHostRefVO.class)
                .select(PrimaryStorageHostRefVO_.primaryStorageUuid)
                .eq(PrimaryStorageHostRefVO_.hostUuid, struct.getHostUuid())
                .listValues();

        if (failureHostRelatedPrimaryStorageUuids.isEmpty()) {
            logger.debug(String.format("no related storage of host[uuid: %s] can be found",
                    struct.getHostUuid()));
            return Collections.emptyList();
        }

        failureHostRelatedPrimaryStorageUuids = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.uuid)
                .eq(PrimaryStorageVO_.type, getPrimaryStorageType().toString())
                .in(PrimaryStorageVO_.uuid, failureHostRelatedPrimaryStorageUuids)
                .listValues();

        if (failureHostRelatedPrimaryStorageUuids.isEmpty()) {
            logger.debug(String.format("no %s related to the failure host[uuid: %s] can be found",
                    getPrimaryStorageType(),
                    struct.getHostUuid()));
            return Collections.emptyList();
        }

        List<String> currentVmUsedPrimaryStorageUuids = struct.getVmInstance()
                .getAllDiskVolumes()
                .stream()
                .map(VolumeInventory::getPrimaryStorageUuid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        currentVmUsedPrimaryStorageUuids.retainAll(failureHostRelatedPrimaryStorageUuids);
        return currentVmUsedPrimaryStorageUuids;
    }

    protected void doFileSystemVmStatusCheck(CheckerStruct struct, String peerHostUuid, HaCheckerCompletion<List<String>> completion) {
        String primaryStorageUuid = struct.getVmInstance().getRootVolume().getPrimaryStorageUuid();
        VmInstanceInventory vm = struct.getVmInstance();

        String mountPath = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, primaryStorageUuid)
                .select(PrimaryStorageVO_.mountPath)
                .findValue();

        CheckFileSystemVmStateCmd cmd = new CheckFileSystemVmStateCmd();
        cmd.interval = struct.getInterval();
        cmd.times = struct.getMaxTimes();
        cmd.primaryStorageUuid = primaryStorageUuid;
        cmd.storageCheckerTimeout = struct.getSuccessTimes();
        cmd.mountPath = mountPath;
        cmd.targetHostUuid = vm.getHostUuid();

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(peerHostUuid);
        msg.setPath(FILESYSTEM_CHECK_VMSTATE_PATH);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());

        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if(!reply.isSuccess()) {
                    logger.debug(String.format("failed to send message check vm[uuid:%s] state of failure on host[uuid: %s]",
                            vm.getUuid(), peerHostUuid));
                    completion.notStable();
                    return;
                }

                KVMHostAsyncHttpCallReply kr = reply.castReply();
                CheckFileSystemVmStateRsp rsp = kr.toResponse(CheckFileSystemVmStateRsp.class);
                if (!rsp.isSuccess()) {
                    logger.debug(String.format("exception occurred when agent check vm[uuid:%s] state of failure on host[uuid: %s]",
                            vm.getUuid(), peerHostUuid));
                    completion.notStable();
                    return;
                }

                Collection<Boolean> res = rsp.result.values();
                if (res.contains(Boolean.TRUE)) {
                    completion.success(rsp.vmUuids);
                    return;
                }
                completion.fail(operr("host[uuid:%s]'s heartbeat is not updated", cmd.targetHostUuid));
            }
        });
    }
}

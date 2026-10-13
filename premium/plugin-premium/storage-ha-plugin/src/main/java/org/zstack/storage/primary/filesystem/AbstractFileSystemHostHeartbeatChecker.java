package org.zstack.storage.primary.filesystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.ha.header.HaErrors;
import org.zstack.ha.message.CheckerStruct;
import org.zstack.ha.spi.HaHostStorageBasedChecker;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.primary.HaStoreFindHostUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.zstack.core.Platform.err;

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

    @Override
    public void check(CheckerStruct struct, HaCheckerCompletion completion) {
        List<String> currentVmUsedPrimaryStorageUuids = HaStoreFindHostUtils.findCurrentVmUsedPrimaryStorageUuids(
                struct, getPrimaryStorageType().toString());
        if (currentVmUsedPrimaryStorageUuids.isEmpty()) {
            completion.noWay();
            return;
        }

        HaStoreFindHostUtils.HeartbeatTarget target = HaStoreFindHostUtils.findHeartbeatTarget(struct,
                Collections.singletonList(struct.vmInstance.getRootVolume().getPrimaryStorageUuid()));
        if (target == null) {
            completion.notStable();
            return;
        }
        doFileSystemVmStatusCheck(struct, target, completion);
    }

    protected void doFileSystemVmStatusCheck(CheckerStruct struct, HaStoreFindHostUtils.HeartbeatTarget target, HaCheckerCompletion<List<String>> completion) {
        String primaryStorageUuid = target.primaryStorageUuid;
        String peerHostUuid = target.hostUuid;
        VmInstanceInventory vm = struct.vmInstance;

        String mountPath = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, primaryStorageUuid)
                .select(PrimaryStorageVO_.mountPath)
                .findValue();

        CheckFileSystemVmStateCmd cmd = new CheckFileSystemVmStateCmd();
        cmd.interval = struct.interval;
        cmd.times = struct.maxTimes;
        cmd.primaryStorageUuid = primaryStorageUuid;
        cmd.storageCheckerTimeout = struct.successTimes;
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
                completion.fail(err(HaErrors.HEARTBEAT_NOT_UPDATED, "host[uuid:%s]'s heartbeat is not updated", cmd.targetHostUuid));
            }
        });
    }
}

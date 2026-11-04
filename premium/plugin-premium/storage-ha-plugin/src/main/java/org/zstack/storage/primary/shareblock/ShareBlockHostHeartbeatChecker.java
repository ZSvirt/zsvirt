package org.zstack.storage.primary.shareblock;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.ha.header.HaErrors;
import org.zstack.ha.message.CheckerStruct;
import org.zstack.ha.spi.HaHostStorageBasedChecker;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.primary.HaStoreFindHostUtils;
import org.zstack.storage.primary.sharedblock.SharedBlockConstants;
import org.zstack.storage.primary.sharedblock.SharedBlockGroupPrimaryStorageHostRefVO;
import org.zstack.storage.primary.sharedblock.SharedBlockGroupPrimaryStorageHostRefVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.zstack.core.Platform.err;

/**
 * @Author: DaoDao
 * @Date: 2023/4/3
 */
public class ShareBlockHostHeartbeatChecker implements HaHostStorageBasedChecker {
    private static final CLogger logger = Utils.getLogger(ShareBlockHostHeartbeatChecker.class);
    public static final String SANLOCK_SCAN_HOST_PATH = "/sharedblock/check/vmstate";

    @Autowired
    private CloudBus bus;

    public static class ShareBlockCheckHostHeartbeatCmd extends KVMAgentCommands.AgentCommand {
        public long interval;
        public long times;
        public String psUuid;
        public String hostUuid;
        public int storageCheckerTimeout;
        public int hostId;
    }

    public static class ShareBlockCheckHostHeartbeatRsp extends KVMAgentCommands.AgentResponse {
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

        HaStoreFindHostUtils.HeartbeatTarget target = HaStoreFindHostUtils.findHeartbeatTarget(struct, currentVmUsedPrimaryStorageUuids);
        if (target == null) {
            completion.noWay();
            return;
        }

        ShareBlockCheckHostHeartbeatCmd cmd = new ShareBlockCheckHostHeartbeatCmd();
        cmd.storageCheckerTimeout = struct.successTimes;
        cmd.interval = struct.interval;
        cmd.times = struct.maxTimes;
        cmd.psUuid = target.primaryStorageUuid;
        cmd.hostUuid = struct.hostUuid;
        Integer hostId = Q.New(SharedBlockGroupPrimaryStorageHostRefVO.class)
                .select(SharedBlockGroupPrimaryStorageHostRefVO_.hostId)
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.hostUuid, cmd.hostUuid)
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid, cmd.psUuid)
                .findValue();
        if (hostId == null) {
            logger.warn(String.format("no sanlock hostId for host[uuid: %s] on primary storage[uuid: %s]", cmd.hostUuid, cmd.psUuid));
            completion.noWay();
            return;
        }
        cmd.hostId = hostId;

        doCheckWithSanlock(target.hostUuid, cmd, completion);
    }

    private void doCheckWithSanlock(String sentinelHostUuid, ShareBlockCheckHostHeartbeatCmd cmd, HaCheckerCompletion<List<String>> completion) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(sentinelHostUuid);
        msg.setPath(SANLOCK_SCAN_HOST_PATH);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.noWay();
                    logger.warn(String.format("call host[uuid: %s] failed: %s", msg.getHostUuid(), reply.getError().getDetails()));
                    return;
                }

                KVMHostAsyncHttpCallReply kr = reply.castReply();
                ShareBlockCheckHostHeartbeatRsp rsp = kr.toResponse(ShareBlockCheckHostHeartbeatRsp.class);
                if (!rsp.isSuccess()) {
                    completion.noWay();
                    logger.warn(String.format("shareblock checker failed on host %s: %s", msg.getHostUuid(), rsp.getError()));
                    return;
                }

                if (rsp.result == null || rsp.result.isEmpty()) {
                    completion.noWay();
                    logger.warn(String.format("shareblock result is empty on host %s", msg.getHostUuid()));
                    return;
                }

                Collection<Boolean> res = rsp.result.values();
                if (res.contains(Boolean.TRUE)) {
                    completion.success(rsp.vmUuids);
                } else {
                    completion.fail(err(HaErrors.HEARTBEAT_NOT_UPDATED,
                            "shareblock says host %s is offline on %s", cmd.hostUuid, sentinelHostUuid));
                }
            }
        });
    }

    @Override
    public int getWeight() {
        return 5;
    }

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return PrimaryStorageType.valueOf(SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE);
    }
}

package org.zstack.storage.primary.shareblock;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.ha.CheckerStruct;
import org.zstack.ha.HaHostStorageBasedChecker;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.primary.sharedblock.SharedBlockConstants;
import org.zstack.storage.primary.sharedblock.SharedBlockGroupPrimaryStorageHostRefVO;
import org.zstack.storage.primary.sharedblock.SharedBlockGroupPrimaryStorageHostRefVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.zstack.core.Platform.operr;

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

    private List<String> findSiblingsFromSameCluster(String hostUuid, String clusterUuid) {
        return Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .notEq(HostVO_.uuid, hostUuid)
                .eq(HostVO_.status, HostStatus.Connected)
                .eq(HostVO_.state, HostState.Enabled)
                .eq(HostVO_.clusterUuid, clusterUuid)
                .limit(3)
                .listValues();
    }

    private boolean isShareBlockPs(String rootVolumePsUuid) {
        long count =  SQL.New("select count(1) from PrimaryStorageVO ps " +
                "where ps.uuid = :uuid and ps.type= :type", Long.class)
                .param("uuid", rootVolumePsUuid)
                .param("type", SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE)
                .find();

        return count > 0;
    }

    @Override
    public void check(CheckerStruct struct, HaCheckerCompletion completion) {
        if (!isShareBlockPs(struct.getVmInstance().getRootVolume().getPrimaryStorageUuid())) {
            completion.noWay();
            return;
        }

        final String clusterUuid = struct.getVmInstance().getClusterUuid();
        final List<String> siblings = findSiblingsFromSameCluster(struct.getHostUuid(), clusterUuid);
        if (siblings.isEmpty()) {
            completion.noWay();
            return;
        }

        Collections.shuffle(siblings);
        ShareBlockCheckHostHeartbeatCmd cmd = new ShareBlockCheckHostHeartbeatCmd();
        cmd.storageCheckerTimeout = struct.getSuccessTimes();
        cmd.interval = struct.getInterval();
        cmd.times = struct.getMaxTimes();
        cmd.psUuid = struct.getVmInstance().getRootVolume().getPrimaryStorageUuid();
        cmd.hostUuid = struct.getHostUuid();
        cmd.hostId = Q.New(SharedBlockGroupPrimaryStorageHostRefVO.class)
                .select(SharedBlockGroupPrimaryStorageHostRefVO_.hostId)
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.hostUuid, cmd.hostUuid)
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid, cmd.psUuid)
                .findValue();

        doCheckWithSanlock(siblings.get(0), cmd, completion);
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
                    completion.fail(operr("shareblock says host %s is offline on %s", cmd.hostUuid, sentinelHostUuid));
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

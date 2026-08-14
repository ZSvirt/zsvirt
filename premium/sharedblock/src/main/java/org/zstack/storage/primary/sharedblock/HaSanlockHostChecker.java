package org.zstack.storage.primary.sharedblock;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.ha.CheckerStruct;
import org.zstack.ha.HaHostStorageBasedChecker;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.ha.HaHostChecker;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class HaSanlockHostChecker implements HaHostStorageBasedChecker {
    private static final CLogger logger = Utils.getLogger(HaSanlockHostChecker.class);
    public static final String SANLOCK_SCAN_HOST_PATH = "/sanlock/scanhost";

    @Autowired
    private CloudBus bus;

    public static class HostIdentifier {
        public HostIdentifier(int hostId, String psUuid) {
            this.hostId = hostId;
            this.psUuid = psUuid;
        }
        public int hostId;
        public String psUuid;
    }

    public static class ScanCmd extends KVMAgentCommands.AgentCommand {
        public long interval;
        public int times;
        public List<HostIdentifier> hostIds;
        public String hostUuid;
        public String psUuid;
    }

    public static class ScanRsp extends KVMAgentCommands.AgentResponse {
        // host id -> online?
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

    // A host can have multiple host ID's.
    // Returns a list of (host_id, psUuid) pairs.
    private List<HostIdentifier> getSanlockHostIds(String hostUuid) {
        final List<Tuple> ts = SQL.New("SELECT ref1.hostId, ref2.primaryStorageUuid FROM SharedBlockGroupPrimaryStorageHostRefVO ref1, PrimaryStorageHostRefVO ref2 "
                + "WHERE ref2.hostUuid = :huuid AND ref2.id = ref1.id", Tuple.class)
                .param("huuid", hostUuid)
                .limit(5)
                .list();
        return ts.stream().map(t -> new HostIdentifier(t.get(0, Integer.class), t.get(1, String.class))).collect(Collectors.toList());
    }

    @Override
    public void check(CheckerStruct struct, HaCheckerCompletion completion) {
        final List<HostIdentifier> hostIds = getSanlockHostIds(struct.getHostUuid());
        if (hostIds.isEmpty()) {
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

        ScanCmd cmd = new ScanCmd();
        cmd.interval = struct.getInterval();
        cmd.times = struct.getMaxTimes();
        cmd.hostIds = hostIds;
        cmd.hostUuid = struct.getHostUuid();
        cmd.psUuid = struct.getVmInstance().getRootVolume().getPrimaryStorageUuid();

        doCheckWithSanlock(siblings.get(0), cmd, completion);
    }

    private void doCheckWithSanlock(String sentinelHostUuid, ScanCmd cmd, HaCheckerCompletion completion) {
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
                ScanRsp rsp = kr.toResponse(ScanRsp.class);
                if (!rsp.isSuccess()) {
                    completion.noWay();
                    logger.warn(String.format("sanlock checker failed on host %s: %s", msg.getHostUuid(), rsp.getError()));
                    return;
                }

                if (rsp.result == null || rsp.result.isEmpty()) {
                    completion.noWay();
                    logger.warn(String.format("sanlock result is empty on host %s", msg.getHostUuid()));
                    return;
                }

                Collection<Boolean> res = rsp.result.values();
                if (res.contains(Boolean.TRUE)) {
                    completion.success(rsp.vmUuids);
                } else {
                    completion.fail(operr("sanlock says host %s is offline on %s", cmd.hostIds, sentinelHostUuid));
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

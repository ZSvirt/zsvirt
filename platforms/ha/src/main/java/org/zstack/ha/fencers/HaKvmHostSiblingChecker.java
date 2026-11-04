package org.zstack.ha.fencers;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.Q;
import org.zstack.ha.header.HaErrors;
import org.zstack.ha.header.HaGlobalConfig;
import org.zstack.ha.message.CheckerStruct;
import org.zstack.ha.spi.HaHostChecker;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.primary.PrimaryStorageSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.zsha2.ZSha2Helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.multiErr;
import static org.zstack.ha.entity.HaCommands.*;
import static org.zstack.utils.CollectionUtils.isEmpty;
import static org.zstack.utils.CollectionUtils.transform;

/**
 * Created by xing5 on 2016/3/29.
 */
public class HaKvmHostSiblingChecker implements HaHostChecker {
    private static final CLogger logger = Utils.getLogger(HaKvmHostSiblingChecker.class);

    @Autowired
    private CloudBus bus;

    private List<String> findSilblingFromSameCluster(String hostUuid, String clusterUuid) {
        Q query = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.status, HostStatus.Connected)
                .eq(HostVO_.state, HostState.Enabled)
                .eq(HostVO_.clusterUuid, clusterUuid)
                .limit(3);
        if (hostUuid != null) {
            query.notEq(HostVO_.uuid, hostUuid);
        }
        return query.listValues();
    }

    private List<String> findSiblingFromOtherCluster(final CheckerStruct struct) {
        if (!HaGlobalConfig.SUPPORT_HA_SLIBING_CROSS_CLUSTERS.value(Boolean.class)) {
            return new ArrayList<>();
        }

        VmInstanceInventory vm = struct.vmInstance;
        List<String> psUuids = vm.getAllVolumes().stream()
                .map(VolumeInventory::getPrimaryStorageUuid)
                .distinct()
                .collect(Collectors.toList());
        Set<String> l2Uuids = new HashSet<>(Q.New(VmNicVO.class, L3NetworkVO.class)
                .table0()
                    .eq(VmNicVO_.vmInstanceUuid, vm.getUuid())
                    .eq(VmNicVO_.l3NetworkUuid).table1(L3NetworkVO_.uuid)
                .table1()
                    .select(L3NetworkVO_.l2NetworkUuid)
                .list());
        if (psUuids.isEmpty() || l2Uuids.isEmpty()) {
            return new ArrayList<>();
        }

        String clusterUuid = vm.getClusterUuid();
        for (String psUuid : psUuids) {
            for (String l2Uuid : l2Uuids) {
                List<String> clusters = Q.New(PrimaryStorageClusterRefVO.class, L2NetworkClusterRefVO.class)
                        .table0()
                            .notEq(PrimaryStorageClusterRefVO_.clusterUuid, clusterUuid)
                            .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, psUuid)
                            .eq(PrimaryStorageClusterRefVO_.clusterUuid).table1(L2NetworkClusterRefVO_.clusterUuid)
                            .select(PrimaryStorageClusterRefVO_.clusterUuid)
                        .table1()
                            .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuid)
                        .list();
                if (clusters.isEmpty()) {
                    continue;
                }
                for (String siblingClusterUuid : clusters) {
                    final List<String> res = findSilblingFromSameCluster(null, siblingClusterUuid);
                    if (!isEmpty(res)) {
                        return res;
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    private List<String> findSiblingHosts(CheckerStruct struct) {
        List<String> candidates = findSilblingFromSameCluster(struct.hostUuid, struct.vmInstance.getClusterUuid());
        if (candidates == null || candidates.isEmpty()) {
            return findSiblingFromOtherCluster(struct);
        } else {
            return candidates;
        }
    }

    private String getIpForScan(final CheckerStruct struct) {
        String psUuid = Q.New(VolumeVO.class)
                .select(VolumeVO_.primaryStorageUuid)
                .eq(VolumeVO_.uuid, struct.vmInstance.getRootVolumeUuid())
                .findValue();
        String ip = getHostStorageAddress(struct.hostUuid, psUuid);
        return ip == null ? struct.hostIp : ip;
    }

    private String getHostStorageAddress(String hostUuid, String psUuid) {
        String vip = getVip();

        final String cidr = PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.getTokenByResourceUuid(
                psUuid, PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY_TOKEN);
        if (cidr == null) {
            logger.warn(String.format("PS[uuid:%s] has no storage network config", psUuid));
            return null;
        }

        final String extraIps = HostSystemTags.EXTRA_IPS.getTokenByResourceUuid(
                hostUuid, HostSystemTags.EXTRA_IPS_TOKEN);
        if (extraIps == null) {
            logger.error(String.format("Host[uuid:%s] has no IPs in storage network", hostUuid));
            return null;
        }

        List<String> ips = Arrays.stream(extraIps.split(","))
                .filter(ip -> !ip.equals(vip))
                .collect(Collectors.toList());

        for (String ip: ips) {
            if (NetworkUtils.isIpv4InCidr(ip, cidr)) {
                return ip;
            }
        }

        return null;
    }


    private String getVip() {
        if (!ZSha2Helper.isMNHaEnvironment()) {
            return null;
        }

        return ZSha2Helper.getInfo(false).getDbvip();
    }

    @Override
    public void check(final CheckerStruct struct, final HaCheckerCompletion completion) {
        List<String> hostUuids =  findSiblingHosts(struct);
        if (hostUuids.isEmpty()) {
            logger.warn(String.format("No sibling hosts found for host: %s", struct.hostUuid));
            completion.noWay();
            return;
        }

        List<KVMHostAsyncHttpCallMsg> msgs = transform(hostUuids, huuid -> {
            ScanCmd cmd = new ScanCmd();
            cmd.hostUuid = struct.hostUuid;
            cmd.slibingHostUuid = huuid;
            cmd.ip = getIpForScan(struct);
            cmd.startPort = 1;
            cmd.endPort = 65535;
            cmd.interval = struct.interval;
            cmd.times = struct.maxTimes;
            cmd.successInterval = struct.successInterval;
            cmd.successTimes = struct.successTimes;

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setHostUuid(huuid);
            msg.setPath(ScanCmd.PATH);
            msg.setCommand(cmd);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, huuid);

            return msg;
        });

        final int total = hostUuids.size();
        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                int success = 0;
                int notStable = 0;
                int failure = 0;
                int noway = 0;
                List<ErrorCode> errors = new ArrayList<ErrorCode>();

                for (MessageReply reply : replies) {
                    if (!reply.isSuccess()) {
                        errors.add(reply.getError());
                    } else {
                        KVMHostAsyncHttpCallReply kr = reply.castReply();
                        ScanRsp rsp = kr.toResponse(ScanRsp.class);
                        if (!rsp.isSuccess()) {
                            errors.add(err(HaErrors.HOST_SCAN_FAILED, "%s", rsp.getError()));
                            noway ++;
                        } else {
                            if (ScanRsp.RET_SUCCESS.equals(rsp.result)) {
                                success ++;
                            } else if (ScanRsp.RET_FAILURE.equals(rsp.result)) {
                                failure ++;
                                errors.add(err(HaErrors.HOST_SCAN_FAILED, "%s", rsp.getError()));
                            } else if (ScanRsp.RET_NOT_STABLE.equals(rsp.result)) {
                                notStable ++;
                            }
                        }
                    }
                }

                logger.debug(String.format("scan host[uuid:%s, ip:%s] result: success: %d, failure: %d, no way: %d, not stable: %d, total: %d",
                        struct.hostUuid, struct.hostIp, success, failure, noway, notStable, total));

                if (success == total) {
                    completion.success(null);
                } else if (failure == total) {
                    completion.fail(multiErr(errors, "hosts failed to port scan the failure host[uuid:%s, ip:%s]",
                            struct.hostUuid, struct.hostIp));
                } else if (noway == total) {
                    completion.noWay();
                } else {
                    completion.notStable();
                }
            }
        });
    }

    @Override
    public int getWeight() {
        return 1;
    }
}

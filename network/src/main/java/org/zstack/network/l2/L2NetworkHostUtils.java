package org.zstack.network.l2;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.network.l2.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

public class L2NetworkHostUtils {
    private static final CLogger logger = Utils.getLogger(L2NetworkHostUtils.class);

    public static void deleteL2NetworkHostRef(String l2Uuid, List<String> hostUuids) {
        if (CollectionUtils.isEmpty(hostUuids)) {
            return;
        }

        logger.debug(String.format("del L2NetworkHostRefVO, l2NetworkUuid: %s, hostUuids: %s",
                l2Uuid, hostUuids));
        SQL.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2Uuid)
                .in(L2NetworkHostRefVO_.hostUuid, hostUuids)
                .delete();
    }

    public static void deleteL2NetworkHostRef(String l2Uuid, String hostUuid) {
        deleteL2NetworkHostRef(l2Uuid, Collections.singletonList(hostUuid));
    }

    public static void deleteL2NetworkHostRef(List<String> l2Uuids, String hostUuid) {
        if (CollectionUtils.isEmpty(l2Uuids)) {
            return;
        }

        logger.debug(String.format("del L2NetworkHostRefVO, l2NetworkUuids: %s, hostUuid: %s",
                l2Uuids, hostUuid));
        SQL.New(L2NetworkHostRefVO.class)
                .in(L2NetworkHostRefVO_.l2NetworkUuid, l2Uuids)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .delete();
    }

    public static String getBridgeNameFromL2NetworkHostRef(String l2Uuid, String hostUuid) {
        return Q.New(L2NetworkHostRefVO.class)
                .select(L2NetworkHostRefVO_.bridgeName)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2Uuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .findValue();
    }

    public static Map<String, String> getBridgeNameMapFromL2NetworkHostRef(List<String> l2Uuids, String hostUuid) {
        Map<String, String> bridgeNameMap = new HashMap<>();
        if (CollectionUtils.isEmpty(l2Uuids)) {
            return bridgeNameMap;
        }

        List<Tuple> tuples = Q.New(L2NetworkHostRefVO.class)
                .select(L2NetworkHostRefVO_.l2NetworkUuid, L2NetworkHostRefVO_.bridgeName)
                .in(L2NetworkHostRefVO_.l2NetworkUuid, l2Uuids)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .listTuple();

        tuples.forEach(t -> {
            String l2Uuid = t.get(0, String.class);
            String bridgeName = t.get(1, String.class);
            bridgeNameMap.put(l2Uuid, bridgeName);
        });

        return bridgeNameMap;
    }

    public static void updateBridgeNameAndSkipDeletion(String l2Uuid, String hostUuid, String bridgeName, boolean skipDeletion) {
        SQL.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2Uuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .set(L2NetworkHostRefVO_.bridgeName, bridgeName)
                .set(L2NetworkHostRefVO_.skipDeletion, skipDeletion)
                .update();
    }

    public static boolean isBridgeDeletionSkippedOnHost(String l2Uuid, String hostUuid) {
        return Q.New(L2NetworkHostRefVO.class)
                .select(L2NetworkHostRefVO_.skipDeletion)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2Uuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .findValue();
    }

    public static boolean checkIfL2AttachedToHost(String l2Uuid, String hostUuid) {
        return Q.New(L2NetworkHostRefVO.class)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2Uuid)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .isExists();
    }

    public static List<String> getNotAttachToAllHostL2Uuids(List<String> l2Uuids) {
        if (CollectionUtils.isEmpty(l2Uuids)) {
            return new ArrayList<>();
        }

        List<Tuple> tuples = Q.New(L2NetworkVO.class)
                .select(L2NetworkVO_.uuid, L2NetworkVO_.type)
                .in(L2NetworkVO_.uuid, l2Uuids)
                .listTuple();

        List<String> notAttachToAllHostsL2Uuids = new ArrayList<>();
        for (Tuple t : tuples) {
            if (!L2NetworkType.valueOf(t.get(1, String.class)).isAttachToAllHosts()) {
                notAttachToAllHostsL2Uuids.add(t.get(0, String.class));
            }
        }
        return notAttachToAllHostsL2Uuids;
    }

    public static List<String> getExcludeHostUuids(List<String> l2Uuids, List<String> hostUuids) {
        if (CollectionUtils.isEmpty(l2Uuids) || CollectionUtils.isEmpty(hostUuids)) {
            return new ArrayList<>();
        }

        List<String> notAttachToAllHostsL2Uuids = getNotAttachToAllHostL2Uuids(l2Uuids);
        if (CollectionUtils.isEmpty(notAttachToAllHostsL2Uuids)) {
            return new ArrayList<>();
        }

        List<Tuple> tuplesWithRef = Q.New(L2NetworkHostRefVO.class)
                .select(L2NetworkHostRefVO_.hostUuid, L2NetworkHostRefVO_.l2NetworkUuid)
                .in(L2NetworkHostRefVO_.l2NetworkUuid, notAttachToAllHostsL2Uuids)
                .in(L2NetworkHostRefVO_.hostUuid, hostUuids)
                .listTuple();
        Map<String, Set<String>> hostWithRefMap = new HashMap<>();
        for (Tuple t : tuplesWithRef) {
            hostWithRefMap.computeIfAbsent(t.get(0, String.class), k -> new HashSet<>()).add(t.get(1, String.class));
        }
        return hostUuids.stream().filter(it ->
                !hostWithRefMap.getOrDefault(it, new HashSet<>()).containsAll(notAttachToAllHostsL2Uuids))
                .collect(Collectors.toList());
    }

    public static List<String> getExcludeL2Uuids(List<String> l2Uuids, String hostUuid) {
        if (CollectionUtils.isEmpty(l2Uuids)) {
            return new ArrayList<>();
        }

        List<String> notAttachToAllHostsL2Uuids = getNotAttachToAllHostL2Uuids(l2Uuids);
        if (CollectionUtils.isEmpty(notAttachToAllHostsL2Uuids)) {
            return new ArrayList<>();
        }

        List<String> l2sWithRef = Q.New(L2NetworkHostRefVO.class)
                .select(L2NetworkHostRefVO_.l2NetworkUuid)
                .in(L2NetworkHostRefVO_.l2NetworkUuid, notAttachToAllHostsL2Uuids)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .listValues();
        return l2Uuids.stream().filter(it -> !l2sWithRef.contains(it)).collect(Collectors.toList());
    }

    public static List<HostVO> getHostsByAttachedL2Network(L2NetworkInventory l2) {
        if (L2NetworkType.valueOf(l2.getType()).isAttachToAllHosts()) {
            List<HostVO> vos = Q.New(HostVO.class)
                    .in(HostVO_.clusterUuid, l2.getAttachedClusterUuids())
                    .notIn(HostVO_.state, asList(HostState.PreMaintenance, HostState.Maintenance))
                    .eq(HostVO_.status, HostStatus.Connected)
                    .list();
            return vos.stream().distinct().collect(Collectors.toList());
        }

        return SQL.New("select distinct host from L2NetworkHostRefVO ref, HostVO host" +
                        " where ref.hostUuid = host.uuid" +
                        " and ref.l2NetworkUuid = :l2Uuid" +
                        " and host.state not in (:states)" +
                        " and host.status = :status ", HostVO.class)
                .param("l2Uuid", l2.getUuid())
                .param("states", asList(HostState.PreMaintenance, HostState.Maintenance))
                .param("status", HostStatus.Connected)
                .list();
    }

    public static ErrorCode validateHostParams(List<HostParam> hostParams, String clusterUuid, String hostUuid) {
        if (CollectionUtils.isEmpty(hostParams)) {
            return null;
        }

        List<String> hostUuids = new ArrayList<>();
        if (!StringUtils.isEmpty(clusterUuid)) {
            hostUuids.addAll(Q.New(HostVO.class).select(HostVO_.uuid)
                    .eq(HostVO_.clusterUuid, clusterUuid)
                    .listValues());

            if (hostUuids.isEmpty()) {
                return argerr("there's no host in cluster[uuid: %s], but hostParams is set", clusterUuid);
            }
        }

        for (HostParam hostParam : hostParams) {
            if (StringUtils.isEmpty(hostParam.getHostUuid())) {
                return argerr("hostUuid can not be null in HostParam");
            }

            if (!CollectionUtils.isEmpty(hostUuids) && !hostUuids.contains(hostParam.getHostUuid())) {
                return argerr("host[uuid: %s] is not in cluster[uuid: %s]", hostParam.getHostUuid(), clusterUuid);
            }

            if (!StringUtils.isEmpty(hostUuid) && !hostUuid.equals(hostParam.getHostUuid())) {
                return argerr("hostUuid in HostParam is inconsistent with the hostUuid in APIMsg");
            }

            if (StringUtils.isEmpty(hostParam.getPhysicalInterface())) {
                return argerr("physical interface can not be null in HostParam");
            }

            if (!StringUtils.isEmpty(hostParam.getL2ProviderType())
                    && !L2ProviderType.hasType(hostParam.getL2ProviderType())) {
                return argerr("unsupported l2Network provider type[%s]", hostParam.getL2ProviderType());
            }
        }

        return null;
    }
}

package org.zstack.storage.primary;


import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.ha.header.HaGlobalConfig;
import org.zstack.ha.message.CheckerStruct;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.volume.VolumeInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class HaStoreFindHostUtils {

    public static class HeartbeatTarget {
        public final String primaryStorageUuid;
        public final String hostUuid;

        public HeartbeatTarget(String primaryStorageUuid, String hostUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
            this.hostUuid = hostUuid;
        }
    }

    public static List<String> findCurrentVmUsedPrimaryStorageUuids(CheckerStruct struct, String storageType) {
        List<String> psUuids = Q.New(PrimaryStorageHostRefVO.class)
                .select(PrimaryStorageHostRefVO_.primaryStorageUuid)
                .eq(PrimaryStorageHostRefVO_.hostUuid, struct.hostUuid)
                .listValues();
        if (psUuids.isEmpty()) {
            return Collections.emptyList();
        }
        psUuids = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.uuid)
                .eq(PrimaryStorageVO_.type, storageType)
                .in(PrimaryStorageVO_.uuid, psUuids)
                .listValues();
        return findVmUsedPrimaryStorageUuids(struct, psUuids);
    }

    public static List<String> findCurrentVmUsedExternalPrimaryStorageUuids(CheckerStruct struct, Collection<String> protocols) {
        if (protocols == null || protocols.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> psUuids = Q.New(PrimaryStorageClusterRefVO.class)
                .select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                .eq(PrimaryStorageClusterRefVO_.clusterUuid, struct.vmInstance.getClusterUuid())
                .listValues();
        if (psUuids.isEmpty()) {
            return Collections.emptyList();
        }
        psUuids = Q.New(ExternalPrimaryStorageVO.class)
                .select(PrimaryStorageVO_.uuid)
                .eq(PrimaryStorageVO_.type, PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE)
                .in(ExternalPrimaryStorageVO_.defaultProtocol, protocols)
                .in(PrimaryStorageVO_.uuid, psUuids)
                .listValues();
        return findVmUsedPrimaryStorageUuids(struct, psUuids);
    }

    private static List<String> findVmUsedPrimaryStorageUuids(CheckerStruct struct, List<String> psUuids) {
        if (psUuids == null || psUuids.isEmpty()) {
            return Collections.emptyList();
        }
        return struct.vmInstance.getAllDiskVolumes().stream()
                .map(VolumeInventory::getPrimaryStorageUuid)
                .filter(Objects::nonNull)
                .distinct()
                .filter(psUuids::contains)
                .collect(Collectors.toList());
    }

    public static HeartbeatTarget findHeartbeatTarget(CheckerStruct struct, List<String> psUuids) {
        if (psUuids == null || psUuids.isEmpty()) {
            return null;
        }
        List<String> candidatePsUuids = psUuids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (candidatePsUuids.isEmpty()) {
            return null;
        }
        Collections.shuffle(candidatePsUuids);
        String rootPsUuid = struct.vmInstance.getRootVolume().getPrimaryStorageUuid();
        if (candidatePsUuids.remove(rootPsUuid)) {
            candidatePsUuids.add(0, rootPsUuid);
        }
        Map<String, List<String>> candidateHostUuidsByPsUuid = findConnectedStorageHostUuids(candidatePsUuids);
        for (String psUuid : candidatePsUuids) {
            List<String> siblings = findSiblingHosts(struct.hostUuid,
                    struct.vmInstance.getClusterUuid(), psUuid,
                    candidateHostUuidsByPsUuid.getOrDefault(psUuid, Collections.emptyList()));
            if (!siblings.isEmpty()) {
                Collections.shuffle(siblings);
                return new HeartbeatTarget(psUuid, siblings.get(0));
            }
        }
        return null;
    }

    public static List<String> findSiblingHosts(String hostUuid, String clusterUuid, String psUuid, List<String> candidateHostUuids) {
        if (psUuid == null) {
            return new ArrayList<>();
        }
        List<String> siblings = findSiblingFromSameCluster(hostUuid, clusterUuid, candidateHostUuids);
        if (siblings.isEmpty()) {
            siblings = findSiblingFromSameCluster(hostUuid, clusterUuid, null);
        }
        if (siblings.isEmpty()) {
            siblings = findSiblingFromOtherCluster(clusterUuid, psUuid, candidateHostUuids);
        }
        if (siblings.isEmpty()) {
            siblings = findSiblingFromOtherCluster(clusterUuid, psUuid, null);
        }
        return siblings;
    }

    private static List<String> findSiblingFromOtherCluster(String clusterUuid, String psUuid, List<String> candidateHostUuids) {
        if (!HaGlobalConfig.SUPPORT_HA_SLIBING_CROSS_CLUSTERS.value(Boolean.class) ||
                (candidateHostUuids != null && candidateHostUuids.isEmpty())) {
            return new ArrayList<>();
        }
        List<L2NetworkClusterRefVO> l2refs = Q.New(L2NetworkClusterRefVO.class)
                .eq(L2NetworkClusterRefVO_.clusterUuid, clusterUuid).list();
        for (L2NetworkClusterRefVO l2ref : l2refs) {
            String sql = "select psref.clusterUuid from PrimaryStorageClusterRefVO psref, L2NetworkClusterRefVO l2ref where " +
                    "psref.clusterUuid != :clusterUuid and l2ref.clusterUuid = psref.clusterUuid and l2ref.l2NetworkUuid = :l2Uuid and psref.primaryStorageUuid = :psUuid";
            List<String> clusters = SQL.New(sql)
                    .param("clusterUuid", clusterUuid)
                    .param("l2Uuid", l2ref.getL2NetworkUuid())
                    .param("psUuid", psUuid).list();
            for (String peerClusterUuid : clusters) {
                List<String> siblings = findSiblingFromSameCluster(null, peerClusterUuid, candidateHostUuids);
                if (!siblings.isEmpty()) {
                    return siblings;
                }
            }
        }
        return new ArrayList<>();
    }

    private static List<String> findSiblingFromSameCluster(String hostUuid, String clusterUuid, List<String> candidateHostUuids) {
        if (candidateHostUuids != null && candidateHostUuids.isEmpty()) {
            return new ArrayList<>();
        }
        Q query = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.status, HostStatus.Connected)
                .eq(HostVO_.state, HostState.Enabled)
                .eq(HostVO_.clusterUuid, clusterUuid)
                .limit(3);
        if (hostUuid != null) {
            query.notEq(HostVO_.uuid, hostUuid);
        }
        if (candidateHostUuids != null) {
            query.in(HostVO_.uuid, candidateHostUuids);
        }
        return query.listValues();
    }

    private static Map<String, List<String>> findConnectedStorageHostUuids(List<String> psUuids) {
        if (psUuids == null || psUuids.isEmpty()) {
            return Collections.emptyMap();
        }
        return Q.New(PrimaryStorageHostRefVO.class)
                .select(PrimaryStorageHostRefVO_.primaryStorageUuid, PrimaryStorageHostRefVO_.hostUuid)
                .in(PrimaryStorageHostRefVO_.primaryStorageUuid, psUuids)
                .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connected)
                .listTuple().stream()
                .collect(Collectors.groupingBy(tuple -> tuple.get(0, String.class),
                        Collectors.mapping(tuple -> tuple.get(1, String.class), Collectors.toList())));
    }
}

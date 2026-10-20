package org.zstack.storage.primary.local;

import org.zstack.core.db.Q;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.i18m;

public class LocalStorageHostRefAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Override
    public void allocate() {
        List<String> psUuids = allPrimaryStorageUuidList();
        if (psUuids.isEmpty()) {
            next();
            return;
        }

        if (spec.getRequiredHostUuid() != null) {
            allocateForHost(spec.getRequiredHostUuid(), psUuids);
        } else if (spec.getRequiredZoneUuid() != null) {
            allocateForZone(spec.getRequiredZoneUuid(), psUuids);
        } else {
            allocateAll(psUuids);
        }
        next();
    }

    private void allocateForHost(String hostUuid, List<String> psUuids) {
        HostVO host = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).find();
        boolean hostOk = host != null
                && host.getState() == HostState.Enabled
                && host.getStatus() == HostStatus.Connected;
        Set<String> attached = new HashSet<>();
        if (hostOk && host.getClusterUuid() != null) {
            attached.addAll(Q.New(PrimaryStorageClusterRefVO.class)
                    .eq(PrimaryStorageClusterRefVO_.clusterUuid, host.getClusterUuid())
                    .select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                    .listValues());
        }

        Map<String, List<LocalStorageHostRefVO>> byPs = hostOk
                ? group(Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, hostUuid)
                .in(LocalStorageHostRefVO_.primaryStorageUuid, psUuids)
                .list())
                : new HashMap<>();

        for (PrimaryStorageCandidate candidate : candidates) {
            List<LocalStorageHostRefVO> refs = byPs.get(candidate.getUuid());
            if (!hostOk || !attached.contains(candidate.getUuid()) || refs == null || refs.isEmpty()) {
                reject(candidate, i18m("the required host[uuid:%s] cannot satisfy conditions[state: %s, status: %s, size > %s bytes]," +
                                " or doesn't belong to a local primary storage satisfying conditions[state: %s, status: %s]," +
                                " or its cluster doesn't attach to any local primary storage",
                        hostUuid, HostState.Enabled, HostStatus.Connected, spec.getSize(),
                        PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected));
            } else {
                LocalStorageCandidateRefs.set(candidate, refs);
            }
        }
    }

    private void allocateForZone(String zoneUuid, List<String> psUuids) {
        Map<String, List<LocalStorageHostRefVO>> byPs = group(enabledHostRefs(psUuids));
        for (PrimaryStorageCandidate candidate : candidates) {
            List<LocalStorageHostRefVO> refs = byPs.get(candidate.getUuid());
            if (!zoneUuid.equals(candidate.primaryStorage.getZoneUuid()) || refs == null || refs.isEmpty()) {
                reject(candidate, i18m("no local primary storage in zone[uuid:%s] can satisfy conditions[state: %s, status: %s]" +
                                " or contain hosts satisfying conditions[state: %s, status: %s, size > %s bytes]",
                        zoneUuid,
                        PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected,
                        HostState.Enabled, HostStatus.Connected, spec.getSize()));
            } else {
                LocalStorageCandidateRefs.set(candidate, refs);
            }
        }
    }

    private void allocateAll(List<String> psUuids) {
        Map<String, List<LocalStorageHostRefVO>> byPs = group(enabledHostRefs(psUuids));
        for (PrimaryStorageCandidate candidate : candidates) {
            List<LocalStorageHostRefVO> refs = byPs.get(candidate.getUuid());
            if (refs == null || refs.isEmpty()) {
                reject(candidate, i18m("no local primary storage can satisfy conditions[state: %s, status: %s]" +
                                " or contain hosts satisfying conditions[state: %s, status: %s, size > %s bytes]",
                        PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected,
                        HostState.Enabled, HostStatus.Connected, spec.getSize()));
            } else {
                LocalStorageCandidateRefs.set(candidate, refs);
            }
        }
    }

    private List<LocalStorageHostRefVO> enabledHostRefs(List<String> psUuids) {
        return Q.New(LocalStorageHostRefVO.class, HostVO.class)
                .table0().selectThisTable()
                .table0().in(LocalStorageHostRefVO_.primaryStorageUuid, psUuids)
                .table0().eq(LocalStorageHostRefVO_.hostUuid).table1(HostVO_.uuid)
                .table1().eq(HostVO_.state, HostState.Enabled)
                .table1().eq(HostVO_.status, HostStatus.Connected)
                .list();
    }

    private Map<String, List<LocalStorageHostRefVO>> group(List<LocalStorageHostRefVO> refs) {
        Map<String, List<LocalStorageHostRefVO>> byPs = new HashMap<>();
        for (LocalStorageHostRefVO ref : refs) {
            byPs.computeIfAbsent(ref.getPrimaryStorageUuid(), key -> new ArrayList<>()).add(ref);
        }
        return byPs;
    }
}

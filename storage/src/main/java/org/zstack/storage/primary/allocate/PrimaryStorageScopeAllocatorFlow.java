package org.zstack.storage.primary.allocate;

import org.zstack.core.db.Q;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostStatus;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.i18m;

public class PrimaryStorageScopeAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Override
    public void allocate() {
        if (spec.getRequiredHostUuid() != null) {
            rejectNotAttachedToHost(spec.getRequiredHostUuid());
        } else if (spec.getRequiredClusterUuids() != null && !spec.getRequiredClusterUuids().isEmpty()) {
            rejectNotAttachedToClusters(spec.getRequiredClusterUuids());
        } else if (spec.getRequiredZoneUuid() != null) {
            rejectOutsideZone(spec.getRequiredZoneUuid());
        }
        next();
    }

    private void rejectNotAttachedToHost(String hostUuid) {
        String clusterUuid = Q.New(HostVO.class)
                .eq(HostVO_.uuid, hostUuid)
                .select(HostVO_.clusterUuid)
                .findValue();
        Set<String> attached = new HashSet<>();
        if (clusterUuid != null) {
            attached.addAll(Q.New(PrimaryStorageClusterRefVO.class)
                    .eq(PrimaryStorageClusterRefVO_.clusterUuid, clusterUuid)
                    .select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                    .listValues());
        }

        List<String> disconnected = Q.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.hostUuid, hostUuid)
                .notEq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Connected)
                .select(PrimaryStorageHostRefVO_.primaryStorageUuid)
                .listValues();
        Set<String> disconnectedUuids = new HashSet<>(disconnected);

        for (PrimaryStorageCandidate candidate : candidates) {
            if (!attached.contains(candidate.getUuid())) {
                reject(candidate, i18m("not attached to the cluster of host[uuid:%s]", hostUuid));
            } else if (disconnectedUuids.contains(candidate.getUuid())) {
                reject(candidate, i18m("not connected to host[uuid:%s]", hostUuid));
            }
        }
    }

    private void rejectNotAttachedToClusters(List<String> clusterUuids) {
        List<String> attached = Q.New(PrimaryStorageClusterRefVO.class)
                .in(PrimaryStorageClusterRefVO_.clusterUuid, clusterUuids)
                .select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                .listValues();
        Set<String> attachedUuids = new HashSet<>(attached);
        for (PrimaryStorageCandidate candidate : candidates) {
            if (!attachedUuids.contains(candidate.getUuid())) {
                reject(candidate, i18m("not attached to clusters%s", clusterUuids));
            }
        }
    }

    private void rejectOutsideZone(String zoneUuid) {
        for (PrimaryStorageCandidate candidate : candidates) {
            if (!zoneUuid.equals(candidate.primaryStorage.getZoneUuid())) {
                reject(candidate, i18m("not in zone[uuid:%s]", zoneUuid));
            }
        }
    }
}

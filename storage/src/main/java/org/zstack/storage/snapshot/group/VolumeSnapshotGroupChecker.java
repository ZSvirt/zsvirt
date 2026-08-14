package org.zstack.storage.snapshot.group;

import org.zstack.core.db.Q;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupAvailability;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.Volume;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18n;

/**
 * Created by MaJin on 2019/7/12.
 */
public class VolumeSnapshotGroupChecker {
    public static boolean isAvailable(String uuid) {
        return getAvailability(uuid).isAvailable();
    }

    public static List<VolumeSnapshotGroupAvailability> getAvailability(List<String> uuids) {
        List<VolumeSnapshotGroupAvailability> results = new ArrayList<>();
        List<VolumeSnapshotGroupVO> groups = Q.New(VolumeSnapshotGroupVO.class)
                .in(VolumeSnapshotGroupVO_.uuid, uuids)
                .list();

        Map<String, String> groupVmRef = groups.stream().collect(Collectors.toMap(ResourceVO::getUuid, VolumeSnapshotGroupVO::getVmInstanceUuid));
        Map<String, Map<String, String>> vmAttachedVols = Q.New(VolumeVO.class)
                .in(VolumeVO_.vmInstanceUuid, groupVmRef.values().stream().distinct().collect(Collectors.toList()))
                .select(VolumeVO_.vmInstanceUuid, VolumeVO_.uuid, VolumeVO_.name).listTuple().stream()
                .collect(Collectors.groupingBy(t -> t.get(0, String.class),
                        Collectors.toMap(it -> ((Tuple) it).get(1, String.class), it -> ((Tuple) it).get(2, String.class))));

        Map<String, Map<String, String>> vmAttachedVolsWithoutMemoryVolume = Q.New(VolumeVO.class)
                .notEq(VolumeVO_.type, VolumeType.Memory)
                .in(VolumeVO_.vmInstanceUuid, groupVmRef.values().stream().distinct().collect(Collectors.toList()))
                .select(VolumeVO_.vmInstanceUuid, VolumeVO_.uuid, VolumeVO_.name).listTuple().stream()
                .collect(Collectors.groupingBy(t -> t.get(0, String.class),
                        Collectors.toMap(it -> ((Tuple) it).get(1, String.class), it -> ((Tuple) it).get(2, String.class))));

        for (VolumeSnapshotGroupVO group : groups) {
            if (!hasMemorySnapshot(group)) {
                results.add(getAvailability(group, vmAttachedVolsWithoutMemoryVolume.get(group.getVmInstanceUuid())));
                continue;
            }

            results.add(getAvailability(group, vmAttachedVols.get(group.getVmInstanceUuid())));
        }

        return results;
    }

    public static VolumeSnapshotGroupAvailability getAvailability(String uuid) {
        VolumeSnapshotGroupVO group = Q.New(VolumeSnapshotGroupVO.class).eq(VolumeSnapshotGroupVO_.uuid, uuid).find();
        return getAvailability(group);
    }

    private static boolean hasMemorySnapshot(VolumeSnapshotGroupVO group) {
        return group.getVolumeSnapshotRefs()
                .stream()
                .anyMatch(ref -> VolumeType.Memory.toString().equals(ref.getVolumeType()));
    }

    public static VolumeSnapshotGroupAvailability getAvailability(VolumeSnapshotGroupVO group) {
        List<Tuple> attachedVolUuids;
        if (hasMemorySnapshot(group)) {
            attachedVolUuids = Q.New(VolumeVO.class).select(VolumeVO_.uuid, VolumeVO_.name)
                    .eq(VolumeVO_.vmInstanceUuid, group.getVmInstanceUuid())
                    .listTuple();
        } else {
            attachedVolUuids = Q.New(VolumeVO.class).select(VolumeVO_.uuid, VolumeVO_.name)
                    .eq(VolumeVO_.vmInstanceUuid, group.getVmInstanceUuid()).notEq(VolumeVO_.type, VolumeType.Memory)
                    .listTuple();
        }
        return getAvailability(group, attachedVolUuids.stream().collect(
                Collectors.toMap(it -> it.get(0, String.class), it -> it.get(1, String.class))));
    }

    private static VolumeSnapshotGroupAvailability getAvailability(VolumeSnapshotGroupVO group, Map<String, String> attachedVolUuidName) {
        List<String> reason = new ArrayList<>();
        List<String> deletedSnapshotInfos = new ArrayList<>();
        List<String> detachedVolInfos = new ArrayList<>();

        Set<String> attachedUuids = new HashSet<>(attachedVolUuidName.keySet());
        Map<String, String> newAttachedVol = new HashMap<>(attachedVolUuidName);
        for (VolumeSnapshotGroupRefVO ref : group.getVolumeSnapshotRefs()) {
            if (ref.isSnapshotDeleted()) {
                deletedSnapshotInfos.add(String.format("[uuid:%s, name:%s]", ref.getVolumeSnapshotUuid(), ref.getVolumeSnapshotName()));
            } else if (!attachedUuids.contains(ref.getVolumeUuid())) {
                detachedVolInfos.add(String.format("[uuid:%s, name:%s]", ref.getVolumeUuid(), ref.getVolumeName()));
            }

            newAttachedVol.remove(ref.getVolumeUuid());
        }

        if (!deletedSnapshotInfos.isEmpty()) {
            reason.add(i18n("snapshot(s) %s in the group has been deleted, can only revert one by one.", String.join(", ", deletedSnapshotInfos)));
        }

        if (!detachedVolInfos.isEmpty()) {
            reason.add(i18n("volume(s) %s is no longer attached, can only revert one by one. " +
                    "If you need to group revert, please re-attach it.", String.join(", ", detachedVolInfos)));
        }

        if (!newAttachedVol.isEmpty()) {
            String volInfos = String.join(", ", newAttachedVol.entrySet().stream().map(e ->
                    String.format("[uuid:%s, name:%s]", e.getKey(), e.getValue()))
                    .collect(Collectors.toList()));
            reason.add(i18n("new volume(s) %s attached after snapshot point, can only revert one by one. " +
                    "If you need to group revert, please detach it.", volInfos));
        }
        return new VolumeSnapshotGroupAvailability(group.getUuid(), String.join("\n", reason));
    }
}

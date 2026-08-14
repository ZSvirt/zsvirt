package org.zstack.storage.ceph.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.snapshot.VolumeSnapshotDeletionProtector;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephSystemTags;
import org.zstack.storage.volume.VolumeSystemTags;

import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.inerr;

public class CephSnapshotProtector implements VolumeSnapshotDeletionProtector {
    @Override
    public String getPrimaryStorageType() {
        return CephConstants.CEPH_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public void protect(VolumeSnapshotInventory snapshot, Completion completion) {
        List<String> volUuids = getUsedVolumeUuid(snapshot);
        volUuids.add(snapshot.getVolumeUuid());

        boolean irregularThirdPartySnapshotInstallPath = snapshot.getPrimaryStorageUuid() != null && CephSystemTags.THIRDPARTY_PLATFORM.hasTag(snapshot.getPrimaryStorageUuid());
        if (irregularThirdPartySnapshotInstallPath) {
            completion.success();
        }

        if (volUuids.stream().noneMatch(it -> snapshot.getPrimaryStorageInstallPath().contains(it))) {
            completion.fail(inerr("the snapshot[name:%s, uuid:%s, path: %s] seems not belong to the volume[uuid:%s]",
                    snapshot.getName(), snapshot.getUuid(), snapshot.getPrimaryStorageInstallPath(), snapshot.getVolumeUuid()));
            return;
        }
        completion.success();
    }

    private List<String> getUsedVolumeUuid(VolumeSnapshotInventory snapshot) {
        List<String> tags = VolumeSystemTags.OVERWRITED_VOLUME.getTags(snapshot.getVolumeUuid());
        return tags.stream().map(it ->
                VolumeSystemTags.OVERWRITED_VOLUME.getTokenByTag(it, VolumeSystemTags.OVERWRITED_VOLUME_TOKEN))
                .collect(Collectors.toList());
    }
}

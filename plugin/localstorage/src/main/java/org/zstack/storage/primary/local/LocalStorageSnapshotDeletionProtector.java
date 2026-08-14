package org.zstack.storage.primary.local;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.snapshot.VolumeSnapshotDeletionProtector;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.storage.snapshot.VolumeSnapshotSystemTags;

import static org.zstack.core.Platform.inerr;
import static org.zstack.core.Platform.operr;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalStorageSnapshotDeletionProtector implements VolumeSnapshotDeletionProtector {
    @Override
    public String getPrimaryStorageType() {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE;
    }

    @Override
    public void protect(VolumeSnapshotInventory snapshot, Completion completion) {
        Path path = Paths.get(snapshot.getPrimaryStorageInstallPath());
        if (!path.getParent().toString().contains(snapshot.getVolumeUuid())) {
            completion.fail(inerr("the snapshot[name:%s, uuid:%s, path: %s] seems not belong to the volume[uuid:%s]",
                    snapshot.getName(), snapshot.getUuid(), snapshot.getPrimaryStorageInstallPath(), snapshot.getVolumeUuid()));
            return;
        }
        completion.success();
    }
}

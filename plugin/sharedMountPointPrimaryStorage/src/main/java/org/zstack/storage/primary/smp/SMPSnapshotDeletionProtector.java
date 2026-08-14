package org.zstack.storage.primary.smp;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.snapshot.VolumeSnapshotDeletionProtector;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.storage.snapshot.VolumeSnapshotSystemTags;

import static org.zstack.core.Platform.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SMPSnapshotDeletionProtector implements VolumeSnapshotDeletionProtector {
    @Override
    public String getPrimaryStorageType() {
        return SMPConstants.SMP_TYPE;
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

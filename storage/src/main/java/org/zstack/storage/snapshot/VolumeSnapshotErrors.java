package org.zstack.storage.snapshot;

/**
 */
public enum VolumeSnapshotErrors {
    NOT_IN_CORRECT_STATE(1000),
    FULL_SNAPSHOT_ERROR(1001),
    BATCH_DELETE_ERROR(1002);

    private String code;

    VolumeSnapshotErrors(int id) {
        code = String.format("VOLUME_SNAPSHOT.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}

package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

/**
 */
public class BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private BackupStorageInventory backupStorage;
    private VolumeSnapshotInventory snapshot;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public BackupStorageInventory getBackupStorage() {
        return backupStorage;
    }

    public void setBackupStorage(BackupStorageInventory backupStorage) {
        this.backupStorage = backupStorage;
    }

    public VolumeSnapshotInventory getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(VolumeSnapshotInventory snapshot) {
        this.snapshot = snapshot;
    }
}

package org.zstack.header.storage.snapshot;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageInventory;

/**
 */
public class BackupVolumeSnapshotMsg extends NeedReplyMessage implements VolumeSnapshotMessage {
    private String snapshotUuid;
    private BackupStorageInventory backupStorage;
    private String volumeUuid;
    /**
     * @ignore
     */
    private String treeUuid;

    @Override
    public String getTreeUuid() {
        return treeUuid;
    }

    @Override
    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    @Override
    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    public BackupStorageInventory getBackupStorage() {
        return backupStorage;
    }

    public void setBackupStorage(BackupStorageInventory backupStorage) {
        this.backupStorage = backupStorage;
    }
}

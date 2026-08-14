package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.VolumeBackupMessage;

public class CancelVolumeBackupJobMsg extends NeedReplyMessage implements VolumeBackupMessage {
    private String volumeUuid;
    private String backupStorageUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}
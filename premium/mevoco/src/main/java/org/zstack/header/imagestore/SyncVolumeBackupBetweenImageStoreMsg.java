package org.zstack.header.imagestore;

import org.zstack.header.message.NeedReplyMessage;

public class SyncVolumeBackupBetweenImageStoreMsg extends NeedReplyMessage {
    private String volumeBackupUuid;
    private String srcImageStorageUuid;
    private String dstImageStorageUuid;
    private String newVolumeBackupUuid;

    public String getVolumeBackupUuid() {
        return volumeBackupUuid;
    }

    public void setVolumeBackupUuid(String volumeBackupUuid) {
        this.volumeBackupUuid = volumeBackupUuid;
    }

    public String getSrcImageStorageUuid() {
        return srcImageStorageUuid;
    }

    public void setSrcImageStorageUuid(String srcImageStorageUuid) {
        this.srcImageStorageUuid = srcImageStorageUuid;
    }

    public String getDstImageStorageUuid() {
        return dstImageStorageUuid;
    }

    public void setDstImageStorageUuid(String dstImageStorageUuid) {
        this.dstImageStorageUuid = dstImageStorageUuid;
    }

    public String getNewVolumeBackupUuid() {
        return newVolumeBackupUuid;
    }

    public void setNewVolumeBackupUuid(String newVolumeBackupUuid) {
        this.newVolumeBackupUuid = newVolumeBackupUuid;
    }
}

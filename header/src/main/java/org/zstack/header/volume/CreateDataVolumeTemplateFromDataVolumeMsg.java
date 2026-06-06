package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

public class CreateDataVolumeTemplateFromDataVolumeMsg extends NeedReplyMessage implements VolumeMessage {
    private String volumeUuid;
    private String backupStorageUuid;
    private String imageUuid;
    private Boolean encrypted;
    private boolean queuedInVolume = true;

    public boolean isQueuedInVolume() {
        return queuedInVolume;
    }

    public void setQueuedInVolume(boolean queuedInVolume) {
        this.queuedInVolume = queuedInVolume;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }
}

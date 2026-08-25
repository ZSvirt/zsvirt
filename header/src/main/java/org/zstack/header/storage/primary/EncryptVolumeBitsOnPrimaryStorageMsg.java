package org.zstack.header.storage.primary;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

public class EncryptVolumeBitsOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String hostUuid;
    private String volumeUuid;
    private String installPath;
    private String targetInstallPath;
    @NoLogging
    private String encryptedDek;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getTargetInstallPath() {
        return targetInstallPath;
    }

    public void setTargetInstallPath(String targetInstallPath) {
        this.targetInstallPath = targetInstallPath;
    }

    public String getEncryptedDek() {
        return encryptedDek;
    }

    public void setEncryptedDek(String encryptedDek) {
        this.encryptedDek = encryptedDek;
    }
}

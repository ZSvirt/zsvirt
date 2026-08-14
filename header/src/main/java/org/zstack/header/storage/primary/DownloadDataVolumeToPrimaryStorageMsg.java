package org.zstack.header.storage.primary;

import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.ReplayableMessage;

public class DownloadDataVolumeToPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage, ReplayableMessage {
    private String primaryStorageUuid;
    private ImageBackupStorageRefInventory backupStorageRef;
    private ImageInventory image;
    private String volumeUuid;
    private String hostUuid;
    private String allocatedInstallUrl;

    public String getAllocatedInstallUrl() {
        return allocatedInstallUrl;
    }

    public void setAllocatedInstallUrl(String allocatedInstallUrl) {
        this.allocatedInstallUrl = allocatedInstallUrl;
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

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public ImageBackupStorageRefInventory getBackupStorageRef() {
        return backupStorageRef;
    }

    public void setBackupStorageRef(ImageBackupStorageRefInventory backupStorageRef) {
        this.backupStorageRef = backupStorageRef;
    }

    public ImageInventory getImage() {
        return image;
    }

    public void setImage(ImageInventory image) {
        this.image = image;
    }

    @Override
    public String getResourceUuid() {
        return volumeUuid;
    }

    @Override
    public Class getReplayableClass() {
        return DownloadDataVolumeToPrimaryStorageMsg.class;
    }
}

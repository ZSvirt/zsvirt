package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 7/6/2015.
 */
public class BackupStorageAskInstallPathMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String imageMediaType;
    private String imageUuid;
    private String backupStorageUuid;
    private String snapshotId; // used by aliyun ebs
    private String zoneId; // used by aliyun ebs

    public String getImageMediaType() {
        return imageMediaType;
    }

    public void setImageMediaType(String imageMediaType) {
        this.imageMediaType = imageMediaType;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }
}

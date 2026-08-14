package org.zstack.storage.primary.sharedblock;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

public class TakeSnapshotOnSharedBlockGroupPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String targetPrimaryStorageUuid;
    private String installPath;
    private String volumeInstallPath;
    private boolean full;
    private String volumeUuid;
    private boolean compareQocw2 = false;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getVolumeInstallPath() {
        return volumeInstallPath;
    }

    public void setVolumeInstallPath(String volumeInstallPath) {
        this.volumeInstallPath = volumeInstallPath;
    }

    public boolean isFull() {
        return full;
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getTargetPrimaryStorageUuid() {
        return targetPrimaryStorageUuid;
    }

    public void setTargetPrimaryStorageUuid(String targetPrimaryStorageUuid) {
        this.targetPrimaryStorageUuid = targetPrimaryStorageUuid;
    }

    public boolean isCompareQocw2() {
        return compareQocw2;
    }

    public void setCompareQocw2(boolean compareQocw2) {
        this.compareQocw2 = compareQocw2;
    }
}

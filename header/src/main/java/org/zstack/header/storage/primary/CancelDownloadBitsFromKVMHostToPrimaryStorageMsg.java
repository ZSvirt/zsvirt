package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

public class CancelDownloadBitsFromKVMHostToPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String primaryStorageInstallPath;
    private String destHostUuid;
    private String longJobUuid;

    public String getDestHostUuid() {
        return destHostUuid;
    }

    public void setDestHostUuid(String destHostUuid) {
        this.destHostUuid = destHostUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPrimaryStorageInstallPath() {
        return primaryStorageInstallPath;
    }

    public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
        this.primaryStorageInstallPath = primaryStorageInstallPath;
    }

    public String getLongJobUuid() {
        return longJobUuid;
    }

    public void setLongJobUuid(String longJobUuid) {
        this.longJobUuid = longJobUuid;
    }
}

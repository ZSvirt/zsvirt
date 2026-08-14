package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

public class UnlinkBitsOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String resourceUuid;
    private String resourceType;
    private String installPath;

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getInstallPath() {
        return installPath;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}

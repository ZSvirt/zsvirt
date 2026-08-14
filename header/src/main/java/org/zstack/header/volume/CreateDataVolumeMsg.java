package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

public class CreateDataVolumeMsg extends NeedReplyMessage implements VolumeCreateMessage {
    private String name;
    private String description;
    private String diskOfferingUuid;
    private long diskSize;
    private String primaryStorageUuid;
    private String accountUuid;
    private String resourceUuid;
    private APICreateDataVolumeMsg apiMsg;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiskOfferingUuid() {
        return diskOfferingUuid;
    }

    public void setDiskOfferingUuid(String diskOfferingUuid) {
        this.diskOfferingUuid = diskOfferingUuid;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public APICreateDataVolumeMsg getApiMsg() {
        return apiMsg;
    }

    public void setApiMsg(APICreateDataVolumeMsg apiMsg) {
        this.apiMsg = apiMsg;
    }
}

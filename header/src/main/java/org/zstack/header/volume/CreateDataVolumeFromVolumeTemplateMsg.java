package org.zstack.header.volume;

import org.zstack.header.message.ConfigurableTimeoutMessage;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.NeedReplyMessage;

import java.util.concurrent.TimeUnit;

/**
 * Create by weiwang at 2018/6/19
 */
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class CreateDataVolumeFromVolumeTemplateMsg extends NeedReplyMessage implements ConfigurableTimeoutMessage {
    private String imageUuid;
    private String name;
    private String description;
    private String primaryStorageUuid;
    private String hostUuid;
    private String resourceUuid;
    private String accountUuid;
    private APICreateDataVolumeFromVolumeTemplateMsg apiMsg;

    public CreateDataVolumeFromVolumeTemplateMsg() {
    }

    public CreateDataVolumeFromVolumeTemplateMsg(APICreateDataVolumeFromVolumeTemplateMsg amsg) {
        imageUuid = amsg.getImageUuid();
        name = amsg.getName();
        description = amsg.getDescription();
        primaryStorageUuid = amsg.getPrimaryStorageUuid();
        hostUuid = amsg.getHostUuid();
        resourceUuid = amsg.getResourceUuid();
        accountUuid = amsg.getSession().getAccountUuid();
        apiMsg = amsg;
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

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
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

    public APICreateDataVolumeFromVolumeTemplateMsg getApiMsg() {
        return apiMsg;
    }

    public void setApiMsg(APICreateDataVolumeFromVolumeTemplateMsg amsg) {
        this.apiMsg = amsg;
    }
}

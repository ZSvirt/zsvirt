package org.zstack.sns;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;

public abstract class APICreateSNSApplicationEndpointMsg extends APICreateMessage implements SNSApplicationPlatformMessage, APIAuditor {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(resourceType = SNSApplicationPlatformVO.class, required = false)
    private String platformUuid;

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

    public String getPlatformUuid() {
        return platformUuid;
    }

    public void setPlatformUuid(String platformUuid) {
        this.platformUuid = platformUuid;
    }

    @Override
    public String getApplicationPlatformUuid() {
        return platformUuid;
    }

    public String getApplicationEndpointType() {
        // null means use application platform type
        return null;
    }

    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateSNSApplicationEndpointEvent)rsp).getInventory().getUuid() : "", SNSApplicationEndpointVO.class);
    }
}

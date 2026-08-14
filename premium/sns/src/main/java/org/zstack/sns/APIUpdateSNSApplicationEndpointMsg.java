package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/sns/application-endpoints/{uuid}/actions", method = HttpMethod.PUT, responseClass = APIUpdateSNSApplicationEndpointEvent.class, isAction = true)
public class APIUpdateSNSApplicationEndpointMsg extends APIMessage implements SNSApplicationEndpointMessage {
    @APIParam(resourceType = SNSApplicationEndpointVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(resourceType = SNSApplicationPlatformVO.class, required = false)
    private String platformUuid;

    public static APIUpdateSNSApplicationEndpointMsg __example__() {
        APIUpdateSNSApplicationEndpointMsg msg = new APIUpdateSNSApplicationEndpointMsg();
        msg.setUuid(uuid());
        msg.setName("new name");
        msg.setDescription("desc");
        msg.setPlatformUuid(uuid());
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getPlatformUuid() {
        return platformUuid;
    }

    public void setPlatformUuid(String platformUuid) {
        this.platformUuid = platformUuid;
    }

    @Override
    public String getApplicationEndpointUuid() {
        return uuid;
    }
}

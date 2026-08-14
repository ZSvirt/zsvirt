package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/sns/application-platforms/{uuid}/actions", method = HttpMethod.PUT, responseClass = APIUpdateSNSApplicationPlatformEvent.class, isAction = true)
public class APIUpdateSNSApplicationPlatformMsg extends APIMessage implements SNSApplicationPlatformMessage {
    @APIParam(resourceType = SNSApplicationPlatformVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public static APIUpdateSNSApplicationPlatformMsg __example__() {
        APIUpdateSNSApplicationPlatformMsg msg = new APIUpdateSNSApplicationPlatformMsg();
        msg.setUuid(uuid(SNSApplicationPlatformVO.class));
        msg.setName("new name");
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

    @Override
    public String getApplicationPlatformUuid() {
        return uuid;
    }
}

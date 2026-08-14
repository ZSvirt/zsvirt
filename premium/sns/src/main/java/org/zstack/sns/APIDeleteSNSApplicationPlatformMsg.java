package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/sns/application-platforms/{uuid}", method = HttpMethod.DELETE, responseClass = APIDeleteSNSApplicationPlatformEvent.class)
public class APIDeleteSNSApplicationPlatformMsg extends APIDeleteMessage implements SNSApplicationPlatformMessage {
    @APIParam(resourceType = SNSApplicationPlatformVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public static APIDeleteSNSApplicationPlatformMsg __example__() {
        APIDeleteSNSApplicationPlatformMsg msg = new APIDeleteSNSApplicationPlatformMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getApplicationPlatformUuid() {
        return uuid;
    }
}

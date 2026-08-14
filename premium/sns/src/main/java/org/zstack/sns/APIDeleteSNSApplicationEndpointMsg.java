package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/sns/application-endpoints/{uuid}", method = HttpMethod.DELETE, responseClass = APIDeleteSNSApplicationEndpointEvent.class)
public class APIDeleteSNSApplicationEndpointMsg extends APIDeleteMessage implements SNSApplicationEndpointMessage {
    @APIParam(resourceType = SNSApplicationEndpointVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public static APIDeleteSNSApplicationEndpointMsg __example__() {
        APIDeleteSNSApplicationEndpointMsg msg = new APIDeleteSNSApplicationEndpointMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public String getApplicationEndpointUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

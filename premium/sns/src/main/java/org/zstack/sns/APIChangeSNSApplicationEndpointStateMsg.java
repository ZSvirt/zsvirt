package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/sns/application-endpoints/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeSNSApplicationEndpointStateEvent.class,
        isAction = true
)
public class APIChangeSNSApplicationEndpointStateMsg extends APIMessage implements SNSApplicationEndpointMessage {
    @APIParam(resourceType = SNSApplicationEndpointVO.class)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public SNSApplicationEndpointStateEvent getStateEvent() {
        return SNSApplicationEndpointStateEvent.valueOf(stateEvent);
    }

    public void setStateEvent(SNSApplicationEndpointStateEvent stateEvent) {
        this.stateEvent = stateEvent.toString();
    }

    @Override
    public String getApplicationEndpointUuid() {
        return uuid;
    }

    public static APIChangeSNSApplicationEndpointStateMsg __example__() {
        APIChangeSNSApplicationEndpointStateMsg msg = new APIChangeSNSApplicationEndpointStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(SNSApplicationEndpointStateEvent.disable);
        return msg;
    }
}

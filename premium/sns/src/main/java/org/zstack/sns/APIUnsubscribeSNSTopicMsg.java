package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/sns/topics/{topicUuid}/endpoints/{endpointUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIUnsubscribeSNSTopicEvent.class
)
public class APIUnsubscribeSNSTopicMsg extends APIMessage implements SNSApplicationEndpointMessage {
    @APIParam(resourceType = SNSTopicVO.class)
    private String topicUuid;
    @APIParam(resourceType = SNSApplicationEndpointVO.class)
    private String endpointUuid;

    public static APIUnsubscribeSNSTopicMsg __example__() {
        APIUnsubscribeSNSTopicMsg msg = new APIUnsubscribeSNSTopicMsg();
        msg.setEndpointUuid(uuid());
        msg.setTopicUuid(uuid());
        return msg;
    }

    public String getTopicUuid() {
        return topicUuid;
    }

    public void setTopicUuid(String topicUuid) {
        this.topicUuid = topicUuid;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }

    @Override
    public String getApplicationEndpointUuid() {
        return endpointUuid;
    }
}

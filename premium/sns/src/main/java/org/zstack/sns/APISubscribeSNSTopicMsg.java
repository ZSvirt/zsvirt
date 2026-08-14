package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/sns/topics/{topicUuid}/endpoints/{endpointUuid}",
        method = HttpMethod.POST,
        responseClass = APISubscribeSNSTopicEvent.class,
        parameterName = "params"
)
public class APISubscribeSNSTopicMsg extends APIMessage implements SNSApplicationEndpointMessage {
    @APIParam(resourceType = SNSTopicVO.class)
    private String topicUuid;
    @APIParam(resourceType = SNSApplicationEndpointVO.class)
    private String endpointUuid;

    public static APISubscribeSNSTopicMsg __example__() {
        APISubscribeSNSTopicMsg msg = new APISubscribeSNSTopicMsg();
        msg.setTopicUuid(uuid());
        msg.setEndpointUuid(uuid());
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

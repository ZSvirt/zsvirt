package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/sns/topics/{uuid}", method = HttpMethod.DELETE, responseClass = APIDeleteSNSTopicEvent.class)
public class APIDeleteSNSTopicMsg extends APIDeleteMessage implements SNSTopicMessage {
    @APIParam(resourceType = SNSTopicVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public static APIDeleteSNSTopicMsg __example__() {
        APIDeleteSNSTopicMsg msg = new APIDeleteSNSTopicMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public String getTopicUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/zwatch/topics/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeSNSTopicStateEvent.class,
        isAction = true)
public class APIChangeSNSTopicStateMsg extends APIMessage implements SNSTopicMessage {
    @APIParam(resourceType = SNSTopicVO.class)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public static APIChangeSNSTopicStateMsg __example__() {
        APIChangeSNSTopicStateMsg msg = new APIChangeSNSTopicStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(SNSTopicStateEvent.disable.toString());
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    @Override
    public String getTopicUuid() {
        return uuid;
    }
}

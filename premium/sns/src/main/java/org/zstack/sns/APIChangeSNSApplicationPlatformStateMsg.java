package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/sns/application-platforms/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeSNSApplicationPlatformStateEvent.class,
        isAction = true
)
public class APIChangeSNSApplicationPlatformStateMsg extends APIMessage implements SNSApplicationPlatformMessage {
    @APIParam(resourceType = SNSApplicationPlatformVO.class)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public static APIChangeSNSApplicationPlatformStateMsg __example__() {
        APIChangeSNSApplicationPlatformStateMsg msg = new APIChangeSNSApplicationPlatformStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(SNSApplicationPlatformStateEvent.enable);
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public SNSApplicationPlatformStateEvent getStateEvent() {
        return SNSApplicationPlatformStateEvent.valueOf(stateEvent);
    }

    public void setStateEvent(SNSApplicationPlatformStateEvent stateEvent) {
        this.stateEvent = stateEvent.toString();
    }

    @Override
    public String getApplicationPlatformUuid() {
        return uuid;
    }
}

package org.zstack.accessKey;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/accesskeys/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeAccessKeyStateEvent.class,
        isAction = true
)
public class APIChangeAccessKeyStateMsg extends APIMessage {
    @APIParam(resourceType = AccessKeyVO.class)
    private String uuid;
    @APIParam(validEnums = AccessKeyStateEvent.class)
    private String stateEvent;

    public static APIChangeAccessKeyStateMsg __example__() {
        APIChangeAccessKeyStateMsg ret = new APIChangeAccessKeyStateMsg();
        ret.setUuid(uuid(AccessKeyVO.class));
        ret.setStateEvent(AccessKeyStateEvent.disable);
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public AccessKeyStateEvent getStateEvent() {
        return AccessKeyStateEvent.valueOf(stateEvent);
    }

    public void setStateEvent(AccessKeyStateEvent stateEvent) {
        this.stateEvent = stateEvent.toString();
    }
}

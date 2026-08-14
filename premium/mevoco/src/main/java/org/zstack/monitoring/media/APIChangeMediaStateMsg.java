package org.zstack.monitoring.media;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/6/11.
 */
@RestRequest(
        path = "/media/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIChangeMediaStateEvent.class
)
@Deprecated
public class APIChangeMediaStateMsg extends APIMessage implements MediaMessage {
    @APIParam(resourceType = MediaVO.class)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

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
    public String getMediaMessageUuid() {
        return uuid;
    }

    public static APIChangeMediaStateMsg __example__() {
        APIChangeMediaStateMsg msg = new APIChangeMediaStateMsg();
        msg.uuid = uuid();
        msg.stateEvent = MediaStateEvent.disable.toString();
        return msg;
    }
}

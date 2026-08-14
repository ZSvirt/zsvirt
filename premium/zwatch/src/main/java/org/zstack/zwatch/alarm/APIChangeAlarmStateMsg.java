package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/zwatch/alarms/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeAlarmStateEvent.class,
        isAction = true
)
public class APIChangeAlarmStateMsg extends APIMessage implements AlarmMessage {
    @APIParam(resourceType = AlarmVO.class)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public static APIChangeAlarmStateMsg __example__() {
        APIChangeAlarmStateMsg ret = new APIChangeAlarmStateMsg();
        ret.setUuid(uuid());
        ret.setStateEvent(AlarmStateEvent.disable);
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public AlarmStateEvent getStateEvent() {
        return AlarmStateEvent.valueOf(stateEvent);
    }

    public void setStateEvent(AlarmStateEvent stateEvent) {
        this.stateEvent = stateEvent.toString();
    }

    @Override
    public String getAlarmUuid() {
        return uuid;
    }
}

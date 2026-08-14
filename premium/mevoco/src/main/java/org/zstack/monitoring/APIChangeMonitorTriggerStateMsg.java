package org.zstack.monitoring;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/6/10.
 */

@RestRequest(
        path = "/monitoring/triggers/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeMonitorTriggerStateEvent.class,
        isAction = true
)
@Deprecated
public class APIChangeMonitorTriggerStateMsg extends APIMessage implements MonitorTriggerMessage {
    @APIParam(resourceType = MonitorTriggerVO.class)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    @Override
    public String getMonitorTriggerUuid() {
        return uuid;
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

    public static APIChangeMonitorTriggerStateMsg __example__() {
        APIChangeMonitorTriggerStateMsg msg = new APIChangeMonitorTriggerStateMsg();
        msg.uuid = uuid();
        msg.stateEvent = MonitorTriggerStateEvent.disable.toString();
        return msg;
    }
}

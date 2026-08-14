package org.zstack.monitoring.actions;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.monitoring.MonitorTriggerStateEvent;

/**
 * Created by xing5 on 2017/6/11.
 */
@RestRequest(
        path = "/monitoring/trigger-actions/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeMonitorTriggerActionStateEvent.class,
        isAction = true
)
@Deprecated
public class APIChangeMonitorTriggerActionStateMsg extends APIMessage implements MonitorTriggerActionMessage {
    @APIParam(resourceType = MonitorTriggerActionVO.class)
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
    public String getMonitorTriggerActionUuid() {
        return uuid;
    }

    public static APIChangeMonitorTriggerActionStateMsg __example__() {
        APIChangeMonitorTriggerActionStateMsg msg = new APIChangeMonitorTriggerActionStateMsg();
        msg.uuid = uuid();
        msg.stateEvent = MonitorTriggerStateEvent.disable.toString();
        return msg;
    }
}

package org.zstack.zwatch.alarm.activealarm.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Create by yaoning at 2020/10/10
 */
@RestRequest(
        path = "/zwatch/activealarms/actions",
        method = HttpMethod.POST,
        responseClass = APIChangeActiveAlarmStateEvent.class,
        parameterName = "params"
)
public class APIChangeActiveAlarmStateMsg extends APIMessage {
    @APIParam
    private String namespace;

    @APIParam(validValues = {"enable","disable"})
    private String stateEvent;

    public static APIChangeActiveAlarmStateMsg __example__() {
        APIChangeActiveAlarmStateMsg msg = new APIChangeActiveAlarmStateMsg();
        msg.setNamespace("ZStack/VM");
        msg.setStateEvent("enable");
        return msg;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }
}

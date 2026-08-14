package org.zstack.monitoring.actions;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/6/11.
 */
@RestRequest(
        path = "/monitoring/trigger-actions/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteMonitorTriggerActionEvent.class
)
@Deprecated
public class APIDeleteMonitorTriggerActionMsg extends APIDeleteMessage implements MonitorTriggerActionMessage {
    @APIParam(resourceType = MonitorTriggerActionVO.class, successIfResourceNotExisting = true)
    private String uuid;

    @Override
    public String getMonitorTriggerActionUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteMonitorTriggerActionMsg __example__() {
        APIDeleteMonitorTriggerActionMsg msg = new APIDeleteMonitorTriggerActionMsg();
        msg.uuid = uuid();
        return msg;
    }
}

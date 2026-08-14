package org.zstack.monitoring;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/6/9.
 */
@RestRequest(
        path = "/monitoring/triggers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteMonitorTriggerEvent.class
)
@Deprecated
public class APIDeleteMonitorTriggerMsg extends APIDeleteMessage implements MonitorTriggerMessage {
    @APIParam(resourceType = MonitorTriggerVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getMonitorTriggerUuid() {
        return uuid;
    }

    public static APIDeleteMonitorTriggerMsg __example__() {
        APIDeleteMonitorTriggerMsg msg = new APIDeleteMonitorTriggerMsg();
        msg.uuid = uuid();
        return msg;
    }
}

package org.zstack.monitoring;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.monitoring.actions.MonitorTriggerActionVO;

/**
 * Created by xing5 on 2017/6/12.
 */
@RestRequest(
        path = "/monitoring/triggers/{triggerUuid}/trigger-actions/{actionUuid}",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAttachMonitorTriggerActionToTriggerEvent.class
)
@Deprecated
public class APIAttachMonitorTriggerActionToTriggerMsg extends APIMessage implements MonitorTriggerMessage {
    @APIParam(resourceType = MonitorTriggerVO.class)
    private String triggerUuid;
    @APIParam(resourceType = MonitorTriggerActionVO.class)
    private String actionUuid;

    public String getTriggerUuid() {
        return triggerUuid;
    }

    public void setTriggerUuid(String triggerUuid) {
        this.triggerUuid = triggerUuid;
    }

    public String getActionUuid() {
        return actionUuid;
    }

    public void setActionUuid(String actionUuid) {
        this.actionUuid = actionUuid;
    }

    @Override
    public String getMonitorTriggerUuid() {
        return triggerUuid;
    }

    public static APIAttachMonitorTriggerActionToTriggerMsg __example__() {
        APIAttachMonitorTriggerActionToTriggerMsg msg = new APIAttachMonitorTriggerActionToTriggerMsg();
        msg.triggerUuid = uuid();
        msg.actionUuid = uuid();
        return msg;
    }
}

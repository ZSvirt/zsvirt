package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.alarm.sns.SNSActionFactory;

@RestRequest(path = "/zwatch/alarms/{alarmUuid}/actions",
        method = HttpMethod.POST,
        responseClass = APIAddActionToAlarmEvent.class,
        parameterName = "params")
public class APIAddActionToAlarmMsg extends APIMessage implements AlarmMessage {
    @APIParam(resourceType = AlarmVO.class)
    private String alarmUuid;
    @APIParam
    private String actionUuid;
    @APIParam
    private String actionType;

    public static APIAddActionToAlarmMsg __example__() {
        APIAddActionToAlarmMsg ret = new APIAddActionToAlarmMsg();
        ret.alarmUuid = uuid();
        ret.actionUuid = uuid();
        ret.actionType = SNSActionFactory.type.toString();
        return ret;
    }

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }

    public String getActionUuid() {
        return actionUuid;
    }

    public void setActionUuid(String actionUuid) {
        this.actionUuid = actionUuid;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
}

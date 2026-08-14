package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/zwatch/alarms/{alarmUuid}/actions/{actionUuid}", method = HttpMethod.DELETE, responseClass = APIRemoveActionFromAlarmEvent.class)
public class APIRemoveActionFromAlarmMsg extends APIDeleteMessage implements AlarmMessage {
    @APIParam(resourceType = AlarmVO.class)
    private String alarmUuid;
    @APIParam
    private String actionUuid;

    public static APIRemoveActionFromAlarmMsg __example__() {
        APIRemoveActionFromAlarmMsg ret = new APIRemoveActionFromAlarmMsg();
        ret.alarmUuid = uuid();
        ret.actionUuid = uuid();
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
}

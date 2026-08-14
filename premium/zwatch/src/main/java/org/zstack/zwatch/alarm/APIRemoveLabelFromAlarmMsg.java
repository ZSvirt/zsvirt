package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/zwatch/alarms/labels/{uuid}", method = HttpMethod.DELETE, responseClass = APIRemoveLabelFromAlarmEvent.class)
public class APIRemoveLabelFromAlarmMsg extends APIDeleteMessage implements AlarmMessage {
    @APIParam(resourceType = AlarmLabelVO.class, successIfResourceNotExisting = true)
    private String uuid;
    @APINoSee
    private String alarmUuid;

    public static APIRemoveLabelFromAlarmMsg __example__() {
        APIRemoveLabelFromAlarmMsg ret = new APIRemoveLabelFromAlarmMsg();
        ret.uuid = uuid();
        ret.alarmUuid = uuid();
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }

    @Override
    public String getAlarmUuid() {
        return alarmUuid;
    }
}

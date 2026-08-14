package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/zwatch/alarms/{uuid}", method = HttpMethod.DELETE, responseClass = APIDeleteAlarmEvent.class)
public class APIDeleteAlarmMsg extends APIDeleteMessage implements AlarmMessage {
    @APIParam(successIfResourceNotExisting = true, resourceType = AlarmVO.class)
    private String uuid;

    public static APIDeleteAlarmMsg __example__() {
        APIDeleteAlarmMsg ret = new APIDeleteAlarmMsg();
        ret.uuid = uuid();
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAlarmUuid() {
        return uuid;
    }
}

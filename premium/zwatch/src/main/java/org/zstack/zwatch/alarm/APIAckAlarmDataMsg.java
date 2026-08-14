package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.ZWatchConstants;

/**
 * Create by yaoning at 2020/10/10
 */
@RestRequest(
        path = "/zwatch/alarm-histories/acknowledgments",
        method = HttpMethod.POST,
        responseClass = APIAckAlertDataEvent.class,
        parameterName = "params"
)
public class APIAckAlarmDataMsg extends APIAckAlertDataMsg {
    @APIParam(maxLength = 32)
    private String alarmUuid;

    public static APIAckAlarmDataMsg __example__() {
        APIAckAlarmDataMsg msg = new APIAckAlarmDataMsg();
        msg.setAlarmUuid(uuid());
        msg.setAlertDataUuid(uuid());
        msg.setDataType(ZWatchConstants.ALARM_DATA_TYPE);
        msg.setResourceUuid(uuid());
        msg.setAckPeriodSec(6000);
        return msg;
    }

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }
}

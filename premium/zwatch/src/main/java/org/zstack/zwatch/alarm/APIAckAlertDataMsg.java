package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.ZWatchConstants;


/**
 * Create by yaoning at 2020/10/10
 */
@RestRequest(
        path = "/zwatch/alert-histories/acknowledgments",
        method = HttpMethod.POST,
        responseClass = APIAckAlertDataEvent.class,
        parameterName = "params"
)
public abstract class APIAckAlertDataMsg extends APIMessage {
    @APIParam(maxLength = 32)
    private String alertDataUuid;

    @APIParam(maxLength = 32, validValues = {ZWatchConstants.ALARM_DATA_TYPE, ZWatchConstants.EVENT_DATA_TYPE})
    private String dataType;

    @APIParam(required = false)
    private String resourceUuid;

    @APIParam
    private Integer ackPeriodSec;

    /*public static APIAddInstanceToMonitorGroupMsg __example__() {
        APIAddInstanceToMonitorGroupMsg msg = new APIAddInstanceToMonitorGroupMsg();
        msg.setName("xsky-172.20.11.24");
        String template = "{\n" +
                "    \"product\":\"XSKY\",\n" +
                "    \"service\":\"XSKY\",\n" +
                "    \"message\":\"${resource_type + '[' + resource_name+'] ' + group + ' ' + alert_value}\",\n" +
                "    \"metric\":\"${resource_type + '::' + group}\",\n" +
                "    \"alertLevel\":\"${level == 'info' ? 'Normal' : level == 'warning' ? 'Important' : 'Emergent'}\",\n" +
                "    \"alertTime\":\"${create}\",\n" +
                "    \"dimensions\":\"{'resource_name':'${resource_name}'}\",\n" +
                "    \"dataSource\":\"xsky-172.20.196.185\"\n" +
                "}";
        msg.setDescription("desc");
        return msg;
    }*/

    public String getAlertDataUuid() {
        return alertDataUuid;
    }

    public void setAlertDataUuid(String alertDataUuid) {
        this.alertDataUuid = alertDataUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public Integer getAckPeriodSec() {
        return ackPeriodSec;
    }

    public void setAckPeriodSec(Integer ackPeriodSec) {
        this.ackPeriodSec = ackPeriodSec;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
}

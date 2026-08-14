package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Create by lining at 2020/10/25
 */
@RestRequest(
        path = "/zwatch/alert-histories/acknowledgments/{alertDataUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateAlertDataAckEvent.class,
        isAction = true)
public class APIUpdateAlertDataAckMsg extends APIMessage {
    @APIParam
    private String alertDataUuid;

    @APIParam(required = false)
    private Boolean resumeAlert;

   public static APIUpdateAlertDataAckMsg __example__() {
       APIUpdateAlertDataAckMsg msg = new APIUpdateAlertDataAckMsg();
       msg.setAlertDataUuid(uuid());
       msg.setResumeAlert(true);
       return msg;
    }

    public String getAlertDataUuid() {
        return alertDataUuid;
    }

    public void setAlertDataUuid(String alertDataUuid) {
        this.alertDataUuid = alertDataUuid;
    }

    public Boolean getResumeAlert() {
        return resumeAlert;
    }

    public void setResumeAlert(Boolean resumeAlert) {
        this.resumeAlert = resumeAlert;
    }
}

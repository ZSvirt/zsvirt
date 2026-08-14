package org.zstack.zwatch.alarm.sns.template.aliyunsms;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.SNSConstants;
import org.zstack.zwatch.alarm.sns.APICreateSNSTextTemplateEvent;
import org.zstack.zwatch.alarm.sns.APICreateSNSTextTemplateMsg;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateVO;

/**
 * Created by Qi Le on 2019-07-13
 */
@RestRequest(
        path = "/zwatch/alarms/sns/text-templates/aliyun-sms",
        method = HttpMethod.POST,
        responseClass = APICreateSNSTextTemplateEvent.class,
        parameterName = "params"
)
public class APICreateAliyunSmsSNSTextTemplateMsg extends APICreateSNSTextTemplateMsg {
    @APIParam
    private String sign;
    @APIParam
    private String alarmTemplateCode;
    @APIParam
    private String eventTemplateCode;
    @APIParam(required = false)
    private String eventTemplate;

    public static APICreateAliyunSmsSNSTextTemplateMsg __example__() {
        APICreateAliyunSmsSNSTextTemplateMsg msg = new APICreateAliyunSmsSNSTextTemplateMsg();
        msg.setName("aliyunSMS Template");
        msg.setApplicationPlatformType(SNSConstants.ALIYUNSMS_PLATFORM);
        msg.setTemplate("Alarm ${ALARM_NAME} changes status to ${ALARM_CURRENT_STATUS}");
        msg.setDefaultTemplate(true);
        msg.alarmTemplateCode = "SMS_153055065";
        msg.eventTemplateCode = "SMS_153055066";
        msg.sign = "举个例子";
        msg.eventTemplate = "Event ${EVENT_NAME} happend.";
        return msg;
    }

    public String getAlarmTemplateCode() {
        return alarmTemplateCode;
    }

    public void setAlarmTemplateCode(String alarmTemplateCode) {
        this.alarmTemplateCode = alarmTemplateCode;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getEventTemplateCode() {
        return eventTemplateCode;
    }

    public void setEventTemplateCode(String eventTemplateCode) {
        this.eventTemplateCode = eventTemplateCode;
    }

    public String getEventTemplate() {
        return eventTemplate;
    }

    public void setEventTemplate(String eventTemplate) {
        this.eventTemplate = eventTemplate;
    }
}

package org.zstack.zwatch.alarm.sns.template.aliyunsms;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.alarm.sns.APIUpdateSNSTextTemplateMsg;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateMessage;

/**
 * Created by Qi Le on 2019-07-15
 */
@RestRequest(
        path = "/zwatch/alarms/sns/text-templates/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateAliyunSmsSNSTextTemplateEvent.class,
        isAction = true
)
public class APIUpdateAliyunSmsSNSTextTemplateMsg extends APIUpdateSNSTextTemplateMsg implements SNSTextTemplateMessage, AliyunSmsSNSTextTemplateMessage {
    @APIParam(required = false)
    private String alarmTemplateCode;
    @APIParam(required = false)
    private String sign;
    @APIParam(required = false)
    private String eventTemplateCode;
    @APIParam(required = false)
    private String eventTemplate;

    public static APIUpdateAliyunSmsSNSTextTemplateMsg __example__() {
        APIUpdateAliyunSmsSNSTextTemplateMsg msg = new APIUpdateAliyunSmsSNSTextTemplateMsg();
        msg.setUuid(uuid());
        msg.setName("aliyunSmsTemplate");
        msg.setDescription("description");
        msg.setTemplate("your aliyun sms alarm template here");
        msg.setDefaultTemplate(true);
        msg.setAlarmTemplateCode("SMS_123456789");
        msg.setEventTemplateCode("SMS_987654321");
        msg.setEventTemplate("your aliyun sms event template here");
        msg.setSign("示例签名");
        return msg;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getAlarmTemplateCode() {
        return alarmTemplateCode;
    }

    public void setAlarmTemplateCode(String alarmTemplateCode) {
        this.alarmTemplateCode = alarmTemplateCode;
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

    @Override
    public String getAlarmTextTemplateUuid() {
        return getUuid();
    }
}

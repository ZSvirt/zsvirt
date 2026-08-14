package org.zstack.zwatch.alarm.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/zwatch/alarms/sns/text-templates/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateSNSTextTemplateEvent.class,
        isAction = true
)
public class APIUpdateSNSTextTemplateMsg extends APIMessage implements SNSTextTemplateMessage {
    @APIParam(resourceType = SNSTextTemplateVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(maxLength = 2048, required = false)
    private String subject;
    @APIParam(maxLength = 2048, required = false)
    private String recoverySubject;
    @APIParam(required = false)
    private String template;
    @APIParam(required = false)
    private String recoveryTemplate;
    @APIParam(required = false)
    private Boolean defaultTemplate;

    public static APIUpdateSNSTextTemplateMsg __example__() {
        APIUpdateSNSTextTemplateMsg ret = new APIUpdateSNSTextTemplateMsg();
        ret.uuid = uuid();
        ret.name = "email template";
        ret.template = "Alarm ${ALARM_NAME} changes status to ${ALARM_CURRENT_STATUS}";
        ret.subject = "Alarm ${ALARM_NAME} changes status to ${ALARM_CURRENT_STATUS}";
        ret.recoverySubject = "Alarm ${ALARM_NAME} ${TITLE_ALARM_RESOURCE_NAME} changes status to ${ALARM_CURRENT_STATUS}";
        ret.defaultTemplate = true;
        ret.recoveryTemplate = "Alarm ${ALARM_NAME} Resource ${ALARM_RESOURCE_NAME} changes status to ${ALARM_CURRENT_STATUS}";
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getRecoverySubject() {
        return recoverySubject;
    }

    public void setRecoverySubject(String recoverySubject) {
        this.recoverySubject = recoverySubject;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Boolean getDefaultTemplate() {
        return defaultTemplate;
    }

    public void setDefaultTemplate(Boolean defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public String getRecoveryTemplate() {
        return recoveryTemplate;
    }

    public void setRecoveryTemplate(String recoveryTemplate) {
        this.recoveryTemplate = recoveryTemplate;
    }

    @Override
    public String getAlarmTextTemplateUuid() {
        return uuid;
    }
}

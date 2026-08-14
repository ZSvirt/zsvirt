package org.zstack.zwatch.alarm.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.platform.email.SNSEmailPlatformFactory;

@RestRequest(
        path = "/zwatch/alarms/sns/text-templates",
        method = HttpMethod.POST,
        responseClass = APICreateSNSTextTemplateEvent.class,
        parameterName = "params"
)
public class APICreateSNSTextTemplateMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam
    private String applicationPlatformType;
    @APIParam(maxLength = 2048, required = false)
    private String subject;
    @APIParam(maxLength = 2048, required = false)
    private String recoverySubject;
    @APIParam
    private String template;
    @APIParam(required = false)
    private String recoveryTemplate;
    private Boolean defaultTemplate;

    @APIParam(required = false, validValues = {"alarm", "event", "combined"})
    private String type;

    public static APICreateSNSTextTemplateMsg __example__() {
        APICreateSNSTextTemplateMsg ret = new APICreateSNSTextTemplateMsg();
        ret.name = "email template";
        ret.applicationPlatformType = SNSEmailPlatformFactory.type.toString();
        ret.subject = "Alarm ${ALARM_NAME} [${ALARM_METRIC} ${ALARM_COMPARISON_OPERATOR} threshold ${ALARM_THRESHOLD}] changes status to ${ALARM_CURRENT_STATUS}";
        ret.recoverySubject = "Alarm ${ALARM_NAME} ${TITLE_ALARM_RESOURCE_NAME} changes status to ${ALARM_CURRENT_STATUS}";
        ret.template = "Alarm ${ALARM_NAME} changes status to ${ALARM_CURRENT_STATUS}";
        ret.defaultTemplate = true;
        ret.recoveryTemplate = "Alarm ${ALARM_NAME} Resource ${ALARM_RESOURCE_NAME} changes status to ${ALARM_CURRENT_STATUS}";
        return ret;
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

    public String getApplicationPlatformType() {
        return applicationPlatformType;
    }

    public void setApplicationPlatformType(String applicationPlatformType) {
        this.applicationPlatformType = applicationPlatformType;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateSNSTextTemplateEvent)rsp).getInventory().getUuid() : "", SNSTextTemplateVO.class);
    }
}

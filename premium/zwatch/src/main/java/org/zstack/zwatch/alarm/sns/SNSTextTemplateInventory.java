package org.zstack.zwatch.alarm.sns;

import org.zstack.core.Platform;
import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;
import org.zstack.sns.platform.email.SNSEmailPlatformFactory;
import org.zstack.utils.StringDSL;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSTextTemplateVO.class)
public class SNSTextTemplateInventory {
    private String uuid;
    private String name;
    private String description;
    private String applicationPlatformType;
    private String subject;
    private String recoverySubject;
    private String template;
    private String recoveryTemplate;
    private Boolean defaultTemplate;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String type;

    public SNSTextTemplateInventory() {
    }

    public SNSTextTemplateInventory(SNSTextTemplateInventory other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.description = other.description;
        this.applicationPlatformType = other.applicationPlatformType;
        this.subject = other.subject;
        this.recoverySubject = other.recoverySubject;
        this.template = other.template;
        this.recoveryTemplate = other.recoveryTemplate;
        this.defaultTemplate = other.defaultTemplate;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
    }

    public static SNSTextTemplateInventory __example__() {
        SNSTextTemplateInventory ret = new SNSTextTemplateInventory();
        ret.uuid = StringDSL.createFixedUuid(SNSTextTemplateVO.class);
        ret.name = "email template";
        ret.applicationPlatformType = SNSEmailPlatformFactory.type.toString();
        ret.subject = "Alarm ${ALARM_NAME} [${ALARM_METRIC} ${ALARM_COMPARISON_OPERATOR} threshold ${ALARM_THRESHOLD}] changes status to ${ALARM_CURRENT_STATUS}";
        ret.recoverySubject = "Alarm ${ALARM_NAME} ${TITLE_ALARM_RESOURCE_NAME} changes status to ${ALARM_CURRENT_STATUS}";
        ret.template = "Alarm ${ALARM_NAME} changes status to ${ALARM_CURRENT_STATUS}";
        ret.recoveryTemplate = "Resource ${EVENT_RESOURCE_ID} changes status to ${ALARM_CURRENT_STATUS}";
        ret.defaultTemplate = false;
        ret.createDate = new Timestamp(DocUtils.date);
        ret.lastOpDate = new Timestamp(DocUtils.date);
        return ret;
    }

    public static SNSTextTemplateInventory valueOf(SNSTextTemplateVO vo) {
        SNSTextTemplateInventory inv = new SNSTextTemplateInventory();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.description = vo.getDescription();
        inv.applicationPlatformType = vo.getApplicationPlatformType();
        inv.subject = vo.getSubject();
        inv.recoverySubject = vo.getRecoverySubject();
        inv.template = vo.getTemplate();
        inv.recoveryTemplate = vo.getRecoveryTemplate();
        inv.defaultTemplate = vo.isDefaultTemplate();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        inv.type = vo.getType().toString();
        return inv;
    }

    public static List<SNSTextTemplateInventory> valueOf(Collection<SNSTextTemplateVO> vos) {
        return vos.stream().map(SNSTextTemplateInventory::valueOf).collect(Collectors.toList());
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

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
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
}

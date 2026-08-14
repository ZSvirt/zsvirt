package org.zstack.zwatch.alarm.sns;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class SNSTextTemplateVO extends ResourceVO implements OwnedByAccount {
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private String applicationPlatformType;
    @Column
    private String subject;
    @Column
    private String recoverySubject;
    @Column
    private String template;
    @Column
    private String recoveryTemplate;
    @Column
    private boolean defaultTemplate;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @Column
    @Enumerated(EnumType.STRING)
    private SNSTextTemplateType type;

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getRecoveryTemplate() {
        return recoveryTemplate;
    }

    public void setRecoveryTemplate(String recoveryTemplate) {
        this.recoveryTemplate = recoveryTemplate;
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

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public boolean isDefaultTemplate() {
        return defaultTemplate;
    }

    public void setDefaultTemplate(boolean defaultTemplate) {
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

    public SNSTextTemplateType getType() {
        return type;
    }

    public void setType(SNSTextTemplateType type) {
        this.type = type;
    }
}

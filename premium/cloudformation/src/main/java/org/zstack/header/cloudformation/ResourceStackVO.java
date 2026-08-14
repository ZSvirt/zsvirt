package org.zstack.header.cloudformation;

import org.zstack.cloudformation.CloudFormationConstant;
import org.zstack.cloudformation.ResourceStackStatus;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
@Entity
@Table
public class ResourceStackVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Transient
    private String accountUuid;
    @Column(length = 128)
    private String name;
    @Column
    private String description;
    @Column
    private String version;
    @Column
    private String type = "zstack";
    @Column(length = CloudFormationConstant.maxLength)
    private String templateContent;
    @Column(length = CloudFormationConstant.paramMaxLength)
    private String paramContent;
    @Column
    @Enumerated(EnumType.STRING)
    private ResourceStackStatus status;
    @Column
    private String reason;
    @Column
    private String outputs;
    @Column
    private boolean enableRollback = false;

    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public ResourceStackStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStackStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        if (reason != null && reason.length() > 2047) {
            this.reason = reason.substring(0, 2047);
        }
        this.reason = reason;
    }

    public boolean isEnableRollback() {
        return enableRollback;
    }

    public void setEnableRollback(boolean enableRollback) {
        this.enableRollback = enableRollback;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParamContent() {
        return paramContent;
    }

    public void setParamContent(String paramContent) {
        this.paramContent = paramContent;
    }

    public String getOutputs() {
        return outputs;
    }

    public void setOutputs(String outputs) {
        this.outputs = outputs;
    }
}

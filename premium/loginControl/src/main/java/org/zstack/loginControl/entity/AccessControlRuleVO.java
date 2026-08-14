package org.zstack.loginControl.entity;

import org.zstack.header.core.encrypt.EncryptColumn;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class AccessControlRuleVO extends ResourceVO {
    @Column
    @Index(length = 128)
    private String name;

    @Column
    private String description;

    @Column
    @EncryptColumn
    private String rule;

    @Column
    @Enumerated(EnumType.STRING)
    @EncryptColumn
    private ControlStrategy strategy;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public ControlStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(ControlStrategy strategy) {
        this.strategy = strategy;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

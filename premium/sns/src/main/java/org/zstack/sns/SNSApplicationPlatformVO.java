package org.zstack.sns;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
public class SNSApplicationPlatformVO extends ResourceVO implements OwnedByAccount, ToInventory {
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private String type;
    @Column
    @Enumerated(EnumType.STRING)
    private SNSApplicationPlatformState state;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

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


    public SNSApplicationPlatformVO() {
    }

    public SNSApplicationPlatformVO(SNSApplicationPlatformVO other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.description = other.description;
        this.accountUuid = other.accountUuid;
        this.type = other.type;
        this.state = other.state;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
    }

    public SNSApplicationPlatformState getState() {
        return state;
    }

    public void setState(SNSApplicationPlatformState state) {
        this.state = state;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
}

package org.zstack.monitoring.media;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/6/11.
 */
@Table
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@BaseResource
public class MediaVO extends ResourceVO implements OwnedByAccount {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String type;

    @Column
    @Enumerated(EnumType.STRING)
    private MediaState state;

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

    public MediaVO() {
    }

    public MediaVO(MediaVO other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.description = other.description;
        this.type = other.type;
        this.state = other.state;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
    }

    @PreUpdate
    void preUpdate() {
        lastOpDate = null;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MediaState getState() {
        return state;
    }

    public void setState(MediaState state) {
        this.state = state;
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

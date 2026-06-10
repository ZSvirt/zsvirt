package org.zstack.zwatch.resnotify;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_ResNotifySubscriptionVO_name", columnNames = {"name"})
})
@AutoDeleteTag
@BaseResource
public class ResNotifySubscriptionVO extends ResourceVO {

    @Column(unique = true)
    private String name;

    @Column
    private String description;

    @Column
    private String resourceTypes;

    @Column
    private String eventTypes;

    @Column
    @Enumerated(EnumType.STRING)
    private ResNotifyType type = ResNotifyType.WEBHOOK;

    @Column
    @Enumerated(EnumType.STRING)
    private ResNotifySubscriptionState state = ResNotifySubscriptionState.Enabled;

    @Column
    private String accountUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    // Getters and Setters

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

    public String getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(String resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public String getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(String eventTypes) {
        this.eventTypes = eventTypes;
    }

    public ResNotifyType getType() {
        return type;
    }

    public void setType(ResNotifyType type) {
        this.type = type;
    }

    public ResNotifySubscriptionState getState() {
        return state;
    }

    public void setState(ResNotifySubscriptionState state) {
        this.state = state;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
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

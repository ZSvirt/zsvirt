package org.zstack.sns;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = SNSApplicationPlatformVO.class, myField = "platformUuid", targetField = "uuid")
        }
)
public class SNSApplicationEndpointVO extends ResourceVO implements OwnedByAccount {
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private String type;
    @Column
    @ForeignKey(parentEntityClass = SNSApplicationPlatformVO.class, parentKey = "uuid")
    private String platformUuid;
    @Column
    @Enumerated(EnumType.STRING)
    private SNSApplicationEndpointState state;
    @Column
    @Enumerated(EnumType.STRING)
    private SNSApplicationEndpointOwnerType ownerType;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;
    @ManyToOne
    @JoinColumn(name = "platformUuid", insertable = false, updatable = false)
    private SNSApplicationPlatformVO platform;
    @Transient
    private String accountUuid;
    @Column
    private String connectionStatus;

    public SNSApplicationEndpointOwnerType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(SNSApplicationEndpointOwnerType ownerType) {
        this.ownerType = ownerType;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }


    public SNSApplicationEndpointVO() {
    }

    public SNSApplicationEndpointVO(SNSApplicationEndpointVO other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.description = other.description;
        this.type = other.type;
        this.platformUuid = other.platformUuid;
        this.accountUuid = other.accountUuid;
        this.state = other.state;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
        this.connectionStatus = other.connectionStatus;
    }

    public SNSApplicationEndpointState getState() {
        return state;
    }

    public void setState(SNSApplicationEndpointState state) {
        this.state = state;
    }

    public String getPlatformUuid() {
        return platformUuid;
    }

    public void setPlatformUuid(String platformUuid) {
        this.platformUuid = platformUuid;
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

    public SNSApplicationPlatformVO getPlatform() {
        return platform;
    }

    public void setPlatform(SNSApplicationPlatformVO platform) {
        this.platform = platform;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }
}

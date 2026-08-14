package org.zstack.mttyDevice;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.*;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @author yu.sun
 * @date 2022/11/16 17:31
 **/
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = HostEO.class, joinColumn = "hostUuid")
})
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid")
        }
)
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
public class MttyDeviceVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    protected String name;

    @Column
    protected String description;

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    protected String hostUuid;
    @Column
    @Enumerated(EnumType.STRING)
    private MttyDeviceState state;

    @Column
    @Enumerated(EnumType.STRING)
    private MttyDeviceVirtStatus virtStatus;

    // the type of this mtty device, like SE...
    @Column
    @Enumerated(EnumType.STRING)
    private MttyDeviceType type;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

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

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public MttyDeviceState getState() {
        return state;
    }

    public void setState(MttyDeviceState state) {
        this.state = state;
    }

    public MttyDeviceVirtStatus getVirtStatus() {
        return virtStatus;
    }

    public void setVirtStatus(MttyDeviceVirtStatus virtStatus) {
        this.virtStatus = virtStatus;
    }

    public MttyDeviceType getType() {
        return type;
    }

    public void setType(MttyDeviceType type) {
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

    @Override
    public String toString() {
        return "MttyDeviceVO{" +
                "state=" + state +
                ", virtStatus=" + virtStatus +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", hostUuid='" + hostUuid + '\'' +
                ", createDate=" + createDate +
                ", lastOpDate=" + lastOpDate +
                ", uuid='" + uuid + '\'' +
                '}';
    }

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
}
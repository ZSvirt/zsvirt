package org.zstack.header.storage.primary;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.ShadowEntity;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 */
@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = PrimaryStorageVO.class, myField = "uuid", targetField = "uuid")
        }
)
public class PrimaryStorageCapacityVO extends StorageCapacityAO implements ShadowEntity {
    @Column
    @Id
    @ForeignKey(parentEntityClass = PrimaryStorageEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String uuid;

    @Column
    @Index
    private long totalCapacity;

    @Column
    @Index
    private long availableCapacity;

    @Column
    private Long systemUsedCapacity;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private PrimaryStorageCapacityVO shadow;

    public PrimaryStorageCapacityVO getShadow() {
        return shadow;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Long getSystemUsedCapacity() {
        return systemUsedCapacity;
    }

    public void setSystemUsedCapacity(Long systemUsedCapacity) {
        this.systemUsedCapacity = systemUsedCapacity;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
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
    public void setShadow(Object o) {
        shadow = (PrimaryStorageCapacityVO) o;
    }

    @Override
    public String getResourceUuid() {
        return uuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }
}

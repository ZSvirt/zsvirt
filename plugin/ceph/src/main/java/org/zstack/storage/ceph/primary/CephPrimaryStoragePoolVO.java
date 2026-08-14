package org.zstack.storage.ceph.primary;

import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/2/28.
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = PrimaryStorageVO.class, joinColumn = "primaryStorageUuid")
})
public class CephPrimaryStoragePoolVO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = PrimaryStorageEO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String primaryStorageUuid;
    @Column
    private String poolName;
    @Column
    private String aliasName;
    @Column
    private String description;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;
    @Column
    private String type;
    @Column
    private long availableCapacity;
    @Column
    private long usedCapacity;
    @Column
    private long totalCapacity;
    @Column
    private String securityPolicy;
    @Column
    private Integer replicatedSize;
    @Column
    private Float diskUtilization;

    @ManyToOne
    @JoinColumn(name = "osdGroupUuid")
    private CephOsdGroupVO osdGroup;

    public CephOsdGroupVO getOsdGroup() {
        return osdGroup;
    }

    public void setOsdGroup(CephOsdGroupVO osdGroup) {
        this.osdGroup = osdGroup;
    }

    public CephPrimaryStoragePoolVO() {
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public long getUsedCapacity() {
        return usedCapacity;
    }

    public void setUsedCapacity(long usedCapacity) {
        this.usedCapacity = usedCapacity;
    }

    public Integer getReplicatedSize() {
        return replicatedSize;
    }

    public void setReplicatedSize(Integer replicatedSize) {
        this.replicatedSize = replicatedSize;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public String getSecurityPolicy() {
        return securityPolicy;
    }

    public void setSecurityPolicy(String securityPolicy) {
        this.securityPolicy = securityPolicy;
    }

    public Float getDiskUtilization() {
        return diskUtilization;
    }

    public void setDiskUtilization(Float diskUtilization) {
        this.diskUtilization = diskUtilization;
    }
}

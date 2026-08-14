package org.zstack.header.vm.metadata;

import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
public class VmMetadataDirtyVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    @ForeignKey(parentEntityClass = ManagementNodeVO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String managementNodeUuid;

    @Column
    private long dirtyVersion = 1;

    @Column
    private Timestamp lastClaimTime;

    @Column
    private boolean storageStructureChange;

    @Column
    private int retryCount;

    @Column
    private Timestamp nextRetryTime;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public long getDirtyVersion() {
        return dirtyVersion;
    }

    public void setDirtyVersion(long dirtyVersion) {
        this.dirtyVersion = dirtyVersion;
    }

    public Timestamp getLastClaimTime() {
        return lastClaimTime;
    }

    public void setLastClaimTime(Timestamp lastClaimTime) {
        this.lastClaimTime = lastClaimTime;
    }

    public boolean isStorageStructureChange() {
        return storageStructureChange;
    }

    public void setStorageStructureChange(boolean storageStructureChange) {
        this.storageStructureChange = storageStructureChange;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Timestamp getNextRetryTime() {
        return nextRetryTime;
    }

    public void setNextRetryTime(Timestamp nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
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

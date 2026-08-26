package org.zstack.header.vm.metadata;

import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class VmMetadataCleanupBarrierVO {
    public static final String SINGLETON_ID = "vm-metadata-cleanup";

    @Id
    @Column
    private String id;

    @Column
    @Enumerated(EnumType.STRING)
    private VmMetadataCleanupBarrierState state;

    @Column
    private String operationUuid;

    @Column
    @ForeignKey(parentEntityClass = ManagementNodeVO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String managementNodeUuid;

    @Column
    private Timestamp leaseExpireDate;

    @Column
    private long generation;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public VmMetadataCleanupBarrierState getState() {
        return state;
    }

    public void setState(VmMetadataCleanupBarrierState state) {
        this.state = state;
    }

    public String getOperationUuid() {
        return operationUuid;
    }

    public void setOperationUuid(String operationUuid) {
        this.operationUuid = operationUuid;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public Timestamp getLeaseExpireDate() {
        return leaseExpireDate;
    }

    public void setLeaseExpireDate(Timestamp leaseExpireDate) {
        this.leaseExpireDate = leaseExpireDate;
    }

    public long getGeneration() {
        return generation;
    }

    public void setGeneration(long generation) {
        this.generation = generation;
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

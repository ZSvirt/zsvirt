package org.zstack.drs.entity;

import org.zstack.drs.data.DRSVmMigrationActivityCause;
import org.zstack.drs.data.DRSVmMigrationStatus;
import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;
import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/12/12.
 */
@Entity
@Table
@BaseResource
public class DRSVmMigrationActivityVO implements ToInventory{
    @Column
    @ForeignKey(parentEntityClass = ClusterDRSVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String drsUuid;

    @Id
    @Column
    @Index
    private String uuid;

    @Column
    private String vmUuid;

    @Column
    private String vmSourceHostUuid;

    @Column
    private String vmTargetHostUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private DRSVmMigrationStatus status;

    @Column
    private String result;

    @Column
    private String reason;

    @Column
    @Enumerated(EnumType.STRING)
    private DRSVmMigrationActivityCause cause;

    @Column
    private String adviceUuid;

    @Column
    private Timestamp endDate;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getDrsUuid() {
        return drsUuid;
    }

    public void setDrsUuid(String drsUuid) {
        this.drsUuid = drsUuid;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getVmSourceHostUuid() {
        return vmSourceHostUuid;
    }

    public void setVmSourceHostUuid(String vmSourceHostUuid) {
        this.vmSourceHostUuid = vmSourceHostUuid;
    }

    public String getVmTargetHostUuid() {
        return vmTargetHostUuid;
    }

    public void setVmTargetHostUuid(String vmTargetHostUuid) {
        this.vmTargetHostUuid = vmTargetHostUuid;
    }

    public DRSVmMigrationStatus getStatus() {
        return status;
    }

    public void setStatus(DRSVmMigrationStatus status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAdviceUuid() {
        return adviceUuid;
    }

    public void setAdviceUuid(String adviceUuid) {
        this.adviceUuid = adviceUuid;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public DRSVmMigrationActivityCause getCause() {
        return cause;
    }

    public void setCause(DRSVmMigrationActivityCause cause) {
        this.cause = cause;
    }
}

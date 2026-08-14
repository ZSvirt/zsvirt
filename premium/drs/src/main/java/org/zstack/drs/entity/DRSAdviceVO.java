package org.zstack.drs.entity;

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
public class DRSAdviceVO implements ToInventory {
    @Id
    @Column
    @Index
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = ClusterDRSVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String drsUuid;

    @Column
    private String adviceGroupUuid;

    @Column
    private String vmUuid;

    @Column
    private String vmSourceHostUuid;

    @Column
    private String vmTargetHostUuid;

    @Column
    private String reason;

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

    public String getAdviceGroupUuid() {
        return adviceGroupUuid;
    }

    public void setAdviceGroupUuid(String adviceGroupUuid) {
        this.adviceGroupUuid = adviceGroupUuid;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
}

package org.zstack.header.vmscheduling;

import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class VmSchedulingRuleRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = VmSchedulingRuleGroupVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmGroupUuid;

    @Column
    @ForeignKey(parentEntityClass = HostSchedulingRuleGroupVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostGroupUuid;

    @Column
    @ForeignKey(parentEntityClass = VmSchedulingRuleVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmSchedulingRuleUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVmGroupUuid() {
        return vmGroupUuid;
    }

    public void setVmGroupUuid(String vmGroupUuid) {
        this.vmGroupUuid = vmGroupUuid;
    }

    public String getHostGroupUuid() {
        return hostGroupUuid;
    }

    public void setHostGroupUuid(String hostGroupUuid) {
        this.hostGroupUuid = hostGroupUuid;
    }

    public String getVmSchedulingRuleUuid() {
        return vmSchedulingRuleUuid;
    }

    public void setVmSchedulingRuleUuid(String vmSchedulingRuleUuid) {
        this.vmSchedulingRuleUuid = vmSchedulingRuleUuid;
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

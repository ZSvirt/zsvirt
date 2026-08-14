package org.zstack.guesttools;

import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Created by GuoYi on 2019-09-19.
 */
@Entity
@Table
public class GuestToolsVO extends ResourceVO implements ToInventory {
    @Column
    @ForeignKey(parentEntityClass = ManagementNodeVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String managementNodeUuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String architecture;

    @Column
    private String hypervisorType;

    @Column
    private String version;

    @Column
    private String agentType;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

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

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuestToolsVO that = (GuestToolsVO) o;
        return Objects.equals(managementNodeUuid, that.managementNodeUuid) &&
                Objects.equals(architecture, that.architecture) &&
                Objects.equals(hypervisorType, that.hypervisorType) &&
                Objects.equals(version, that.version) &&
                Objects.equals(agentType, that.agentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(managementNodeUuid, architecture, hypervisorType, version, agentType);
    }
}

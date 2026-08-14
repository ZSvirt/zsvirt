package org.zstack.sdk;



public class VmNicSecurityGroupRefInventory  {

    public java.lang.Integer priority;
    public void setPriority(java.lang.Integer priority) {
        this.priority = priority;
    }
    public java.lang.Integer getPriority() {
        return this.priority;
    }

    public java.lang.String vmNicUuid;
    public void setVmNicUuid(java.lang.String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }
    public java.lang.String getVmNicUuid() {
        return this.vmNicUuid;
    }

    public java.lang.String securityGroupUuid;
    public void setSecurityGroupUuid(java.lang.String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }
    public java.lang.String getSecurityGroupUuid() {
        return this.securityGroupUuid;
    }

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}

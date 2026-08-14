package org.zstack.sdk;



public class VmNicSecurityPolicyInventory  {

    public java.lang.String vmNicUuid;
    public void setVmNicUuid(java.lang.String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }
    public java.lang.String getVmNicUuid() {
        return this.vmNicUuid;
    }

    public java.lang.String ingressPolicy;
    public void setIngressPolicy(java.lang.String ingressPolicy) {
        this.ingressPolicy = ingressPolicy;
    }
    public java.lang.String getIngressPolicy() {
        return this.ingressPolicy;
    }

    public java.lang.String egressPolicy;
    public void setEgressPolicy(java.lang.String egressPolicy) {
        this.egressPolicy = egressPolicy;
    }
    public java.lang.String getEgressPolicy() {
        return this.egressPolicy;
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

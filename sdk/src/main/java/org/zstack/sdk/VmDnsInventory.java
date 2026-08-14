package org.zstack.sdk;



public class VmDnsInventory  {

    public java.lang.String vmInstanceUuid;
    public void setVmInstanceUuid(java.lang.String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public java.lang.String getVmInstanceUuid() {
        return this.vmInstanceUuid;
    }

    public java.lang.String vmNicUuid;
    public void setVmNicUuid(java.lang.String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }
    public java.lang.String getVmNicUuid() {
        return this.vmNicUuid;
    }

    public java.lang.String dns;
    public void setDns(java.lang.String dns) {
        this.dns = dns;
    }
    public java.lang.String getDns() {
        return this.dns;
    }

    public java.lang.Integer ipVersion;
    public void setIpVersion(java.lang.Integer ipVersion) {
        this.ipVersion = ipVersion;
    }
    public java.lang.Integer getIpVersion() {
        return this.ipVersion;
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

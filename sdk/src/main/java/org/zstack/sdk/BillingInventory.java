package org.zstack.sdk;



public class BillingInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String billingType;
    public void setBillingType(java.lang.String billingType) {
        this.billingType = billingType;
    }
    public java.lang.String getBillingType() {
        return this.billingType;
    }

    public java.lang.String accountUuid;
    public void setAccountUuid(java.lang.String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public java.lang.String getAccountUuid() {
        return this.accountUuid;
    }

    public java.lang.String resourceUuid;
    public void setResourceUuid(java.lang.String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
    public java.lang.String getResourceUuid() {
        return this.resourceUuid;
    }

    public java.lang.String resourceName;
    public void setResourceName(java.lang.String resourceName) {
        this.resourceName = resourceName;
    }
    public java.lang.String getResourceName() {
        return this.resourceName;
    }

    public double spending;
    public void setSpending(double spending) {
        this.spending = spending;
    }
    public double getSpending() {
        return this.spending;
    }

    public long startTime;
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    public long getStartTime() {
        return this.startTime;
    }

    public long endTime;
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    public long getEndTime() {
        return this.endTime;
    }

    public java.lang.String hypervisorType;
    public void setHypervisorType(java.lang.String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
    public java.lang.String getHypervisorType() {
        return this.hypervisorType;
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

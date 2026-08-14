package org.zstack.sdk.identity.role;



public class RoleAccountRefInventory  {

    public java.lang.String roleUuid;
    public void setRoleUuid(java.lang.String roleUuid) {
        this.roleUuid = roleUuid;
    }
    public java.lang.String getRoleUuid() {
        return this.roleUuid;
    }

    public java.lang.String accountUuid;
    public void setAccountUuid(java.lang.String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public java.lang.String getAccountUuid() {
        return this.accountUuid;
    }

    public java.lang.String accountPermissionFrom;
    public void setAccountPermissionFrom(java.lang.String accountPermissionFrom) {
        this.accountPermissionFrom = accountPermissionFrom;
    }
    public java.lang.String getAccountPermissionFrom() {
        return this.accountPermissionFrom;
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

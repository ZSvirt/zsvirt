package org.zstack.sdk.iam1.ensemble;



public class GetResourceSharingResult {
    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String masterUuid;
    public void setMasterUuid(java.lang.String masterUuid) {
        this.masterUuid = masterUuid;
    }
    public java.lang.String getMasterUuid() {
        return this.masterUuid;
    }

    public java.lang.String masterResourceType;
    public void setMasterResourceType(java.lang.String masterResourceType) {
        this.masterResourceType = masterResourceType;
    }
    public java.lang.String getMasterResourceType() {
        return this.masterResourceType;
    }

    public boolean toPublic;
    public void setToPublic(boolean toPublic) {
        this.toPublic = toPublic;
    }
    public boolean getToPublic() {
        return this.toPublic;
    }

    public java.util.List accounts;
    public void setAccounts(java.util.List accounts) {
        this.accounts = accounts;
    }
    public java.util.List getAccounts() {
        return this.accounts;
    }

    public java.util.List accountGroups;
    public void setAccountGroups(java.util.List accountGroups) {
        this.accountGroups = accountGroups;
    }
    public java.util.List getAccountGroups() {
        return this.accountGroups;
    }

}

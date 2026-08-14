package org.zstack.sdk.identity.ldap.compute;



public class LdapSyncTaskResult extends org.zstack.sdk.identity.imports.header.SyncTaskResult {

    public java.lang.String stage;
    public void setStage(java.lang.String stage) {
        this.stage = stage;
    }
    public java.lang.String getStage() {
        return this.stage;
    }

    public int completeCount;
    public void setCompleteCount(int completeCount) {
        this.completeCount = completeCount;
    }
    public int getCompleteCount() {
        return this.completeCount;
    }

    public int totalCount;
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    public int getTotalCount() {
        return this.totalCount;
    }

}

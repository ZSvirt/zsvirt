package org.zstack.ldap.compute;

import org.zstack.identity.imports.header.SyncTaskResult;
import org.zstack.ldap.LdapConstant;

public class LdapSyncTaskResult extends SyncTaskResult {
    {
        setSourceType(LdapConstant.LOGIN_TYPE);
    }

    public String stage;
    public int completeCount;
    public int totalCount;

    public LdapSyncTaskResult withLdapServer(String ldapServerUuid) {
        this.setSourceUuid(ldapServerUuid);
        return this;
    }

    public LdapSyncTaskResult withStage(String stage) {
        this.stage = stage;
        return this;
    }

    public LdapSyncTaskResult withExistingRecordCount(long existCount) {
        cleanStage.setTotal((int) existCount);
        setTotalCount(importStage.getTotal() + cleanStage.getTotal());
        return this;
    }

    public LdapSyncTaskResult withSearchRecordCount(int searchCount) {
        importStage.setTotal(searchCount);
        setTotalCount(importStage.getTotal() + cleanStage.getTotal());
        return this;
    }

    public synchronized LdapSyncTaskResult appendFailCountInImportStage(int failCount) {
        this.completeCount += failCount;
        this.importStage.setFail(importStage.getFail() + failCount);
        return this;
    }

    public synchronized LdapSyncTaskResult appendSuccessCountInImportStage(int successCount) {
        this.completeCount += successCount;
        this.importStage.setSuccess(importStage.getSuccess() + successCount);
        return this;
    }

    public synchronized LdapSyncTaskResult appendFailCountInCleanStage(int failCount) {
        this.completeCount += failCount;
        this.cleanStage.setFail(cleanStage.getFail() + failCount);
        return this;
    }

    public synchronized LdapSyncTaskResult appendSuccessCountInCleanStage(int successCount) {
        this.completeCount += successCount;
        this.cleanStage.setSuccess(cleanStage.getSuccess() + successCount);
        return this;
    }

    public synchronized LdapSyncTaskResult appendSkipCountInCleanStage(int skipCount) {
        this.completeCount += skipCount;
        this.cleanStage.setSkip(cleanStage.getSkip() + skipCount);
        return this;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public int getCompleteCount() {
        return completeCount;
    }

    public void setCompleteCount(int completeCount) {
        this.completeCount = completeCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}

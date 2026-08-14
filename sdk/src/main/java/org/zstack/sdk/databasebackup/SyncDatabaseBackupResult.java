package org.zstack.sdk.databasebackup;

import org.zstack.sdk.SyncBackupResult;

public class SyncDatabaseBackupResult {
    public SyncBackupResult result;
    public void setResult(SyncBackupResult result) {
        this.result = result;
    }
    public SyncBackupResult getResult() {
        return this.result;
    }

}

package org.zstack.sdk.storage.volumebackup;

import org.zstack.sdk.SyncBackupResult;

public class SyncVolumeBackupResult {
    public SyncBackupResult result;
    public void setResult(SyncBackupResult result) {
        this.result = result;
    }
    public SyncBackupResult getResult() {
        return this.result;
    }

}

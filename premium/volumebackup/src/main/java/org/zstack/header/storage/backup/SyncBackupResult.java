package org.zstack.header.storage.backup;

/**
 * Created by MaJin on 2019/10/30.
 */
public class SyncBackupResult {
    public int deletedBackupCount = 0;
    public int newBackupCount = 0;

    public int getDeletedBackupCount() {
        return deletedBackupCount;
    }

    public void setDeletedBackupCount(int deletedBackupCount) {
        this.deletedBackupCount = deletedBackupCount;
    }

    public int getNewBackupCount() {
        return newBackupCount;
    }

    public void setNewBackupCount(int newBackupCount) {
        this.newBackupCount = newBackupCount;
    }

    public SyncBackupResult() {
    }

    public SyncBackupResult(int deletedBackupCount, int newBackupCount) {
        this.deletedBackupCount = deletedBackupCount;
        this.newBackupCount = newBackupCount;
    }
}

package org.zstack.sdk;



public class SyncBackupResult  {

    public int deletedBackupCount;
    public void setDeletedBackupCount(int deletedBackupCount) {
        this.deletedBackupCount = deletedBackupCount;
    }
    public int getDeletedBackupCount() {
        return this.deletedBackupCount;
    }

    public int newBackupCount;
    public void setNewBackupCount(int newBackupCount) {
        this.newBackupCount = newBackupCount;
    }
    public int getNewBackupCount() {
        return this.newBackupCount;
    }

}

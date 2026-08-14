package org.zstack.header.storage.backup;

public class BackupRetentionPolicy {
    private BackupRetentionType retentionType;

    private long retentionValue;
    private long fullBackupRetentionValue;

    public BackupRetentionType getRetentionType() {
        return retentionType;
    }

    public void setRetentionType(BackupRetentionType retentionType) {
        this.retentionType = retentionType;
    }

    public long getRetentionValue() {
        return retentionValue;
    }

    public void setRetentionValue(long retentionValue) {
        this.retentionValue = retentionValue;
    }

    public long getFullBackupRetentionValue() {
        return fullBackupRetentionValue;
    }

    public void setFullBackupRetentionValue(long fullBackupRetentionValue) {
        this.fullBackupRetentionValue = fullBackupRetentionValue;
    }
}

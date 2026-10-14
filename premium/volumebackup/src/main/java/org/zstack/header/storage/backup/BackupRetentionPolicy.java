package org.zstack.header.storage.backup;

import org.apache.logging.log4j.util.Strings;

import java.util.Objects;

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

    public boolean initialized() {
        return retentionValue != 0 || retentionType != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackupRetentionPolicy that = (BackupRetentionPolicy) o;
        return retentionValue == that.retentionValue && fullBackupRetentionValue == that.fullBackupRetentionValue
                && retentionType == that.retentionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(retentionType, retentionValue, fullBackupRetentionValue);
    }

    public static BackupRetentionPolicy valueOf(String retentionType, String retentionValue) {
        BackupRetentionPolicy policy = new BackupRetentionPolicy();
        policy.setRetentionType(Strings.isBlank(retentionType) ? null : BackupRetentionType.valueOf(retentionType));
        policy.setRetentionValue(Strings.isBlank(retentionValue) ? 0 : Long.parseLong(retentionValue));
        return policy.initialized() ? policy : null;

    }
}

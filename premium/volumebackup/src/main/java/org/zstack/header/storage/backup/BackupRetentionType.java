package org.zstack.header.storage.backup;

public enum BackupRetentionType {
    /**
     * Keep volume backup within a specified count
     */
    Count,

    /**
     * Keep volume backup with given days
     */
    Days,
}

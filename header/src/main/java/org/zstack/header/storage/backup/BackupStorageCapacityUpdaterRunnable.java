package org.zstack.header.storage.backup;

/**
 * Created by xing5 on 2016/4/28.
 */
public interface BackupStorageCapacityUpdaterRunnable {
    BackupStorageCapacity call(BackupStorageCapacity cap);
}

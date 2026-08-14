package org.zstack.header.storage.primary;

import java.util.Map;

/**
 * Created by mingjian.deng on 2017/11/3.
 */
public interface PrimaryStorageToBackupStorageMediatorExtensionPoint {
    Map<String, BackupStorageMediator> getBackupStorageMediators();
}

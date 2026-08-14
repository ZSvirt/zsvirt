package org.zstack.header.storage.backup;

import java.util.List;

/**
 * Created by xing5 on 2016/9/19.
 */
public interface BackupStorageFindRelatedPrimaryStorage {
    List<String> findRelatedPrimaryStorage(String backupStorageUuid);
}

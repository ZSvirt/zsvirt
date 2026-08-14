package org.zstack.header.storage.primary;

import java.util.List;

/**
 * Created by xing5 on 2016/9/19.
 */
public interface PrimaryStorageFindBackupStorage {
    List<String> findBackupStorage(String primaryStorageUuid);
}

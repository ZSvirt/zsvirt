package org.zstack.header.storage.backup;

import java.util.List;

/**
 * Created by LiangHanYu on 2023/2/10 17:48
 */
public interface CleanUpVmBackupExtensionPoint {
    void afterCleanUpVmBackup(String groupUuid);
}

package org.zstack.header.storage.volume.backup;

import java.util.List;

/**
 * Created by MaJin on 2019/4/28.
 */
public interface DeleteVolumeBackupExtensionPoint {
    void afterDeleteVolumeBackup(String backupUuid, List<String> bsUuids);
}

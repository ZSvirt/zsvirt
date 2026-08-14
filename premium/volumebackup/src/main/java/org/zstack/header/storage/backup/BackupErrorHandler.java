package org.zstack.header.storage.backup;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.storage.backup.CreateVmBackupJob;
import org.zstack.storage.backup.CreateVolumeBackupJob;

/**
 * Created by MaJin on 2019/5/29.
 */
public interface BackupErrorHandler {
    void handle(ErrorCode error, CreateVmBackupJob job);
    void handle(ErrorCode error, CreateVolumeBackupJob job);
}

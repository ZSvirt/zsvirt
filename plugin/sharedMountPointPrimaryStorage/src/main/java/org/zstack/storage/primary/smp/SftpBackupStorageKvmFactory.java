package org.zstack.storage.primary.smp;

import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:04 2023/7/7
 */
public class SftpBackupStorageKvmFactory implements BackupStorageKvmFactory {
    @Override
    public String getBackupStorageType() {
        return SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE;
    }

    @Override
    public BackupStorageKvmUploader createUploader(PrimaryStorageInventory ps, String bsUuid) {
        return SftpBackupStorageKvmUploader.createUploader(ps, bsUuid);
    }

    @Override
    public BackupStorageKvmDownloader createDownloader(PrimaryStorageInventory ps, String bsUuid) {
        return SftpBackupStorageKvmDownloader.createDownloader(ps, bsUuid);
    }
}

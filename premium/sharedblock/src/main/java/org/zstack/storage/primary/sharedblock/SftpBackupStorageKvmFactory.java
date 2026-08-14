package org.zstack.storage.primary.sharedblock;

import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;

public class SftpBackupStorageKvmFactory implements BackupStorageSharedBlockKvmFactory {
    @Override
    public String getBackupStorageType() {
        return SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE;
    }

    @Override
    public BackupStorageSharedBlockKvmUploader createUploader(PrimaryStorageInventory ps, String bsUuid) {
        return SftpBackupStorageKvmUploader.createUploader(ps, bsUuid);
    }

    @Override
    public BackupStorageSharedBlockKvmDownloader createDownloader(PrimaryStorageInventory ps, String bsUuid) {
        return SftpBackupStorageKvmDownloader.createDownloader(ps, bsUuid);
    }
}

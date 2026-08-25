package org.zstack.storage.backup.imagestore;

import org.zstack.header.errorcode.ErrorableValue;

public interface ImageStoreBackupStorageManager {
    String findImageUuid(String backupStorageUuid, String installPath);

    String findVolumeBackupUuid(String backupStorageUuid, String installPath);

    String findVolumeBackupUuidByImageUuid(String imageUuid);

    String findVolumeBackupUuidByImageUuidWithMetadataFallback(String imageUuid);

    long countImageBackupStorageRefs(String imageUuid);

    ErrorableValue<String> findConnectedKvmHost(String primaryStorageUuid, String storageDescription, String operation);
}

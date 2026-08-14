package org.zstack.header.storage.backup;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by Mei Lei <meilei007@gmail.com> on 11/5/16.
 */
public interface AddBackupStorageExtensionPoint {
    void preAddBackupStorage(AddBackupStorageStruct backupStorage);

    void beforeAddBackupStorage(AddBackupStorageStruct backupStorage);

    void afterAddBackupStorage(AddBackupStorageStruct backupStorage);

    void failedToAddBackupStorage(AddBackupStorageStruct backupStorage, ErrorCode err);
}

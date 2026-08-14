package org.zstack.storage.primary.sharedblock;

import org.zstack.header.core.ReturnValueCompletion;

public abstract class BackupStorageSharedBlockKvmUploader {
    // When uploading an image from 'psPath' to backup storage, the 'bsPath'
    // might be allocated by the backup storage and returned by the completion,
    // instead of being known ahead of time.
    public abstract void uploadBits(String imageUuid, String bsPath, String psPath, ReturnValueCompletion<String> completion);
    public abstract void uploadBits(String imageUuid, String bsPath, String psPath, String hostUuid, ReturnValueCompletion<String> completion);
}

package org.zstack.storage.primary.smp;

import org.zstack.header.core.ReturnValueCompletion;

/**
 * Created by xing5 on 2016/3/27.
 */
public abstract class BackupStorageKvmUploader {
    // When uploading an image from 'psPath' to backup storage, the 'bsPath'
    // might be allocated by the backup storage and returned by the completion,
    // instead of being known ahead of time.
    public abstract void uploadBits(String imageUuid, String bsPath, String psPath, ReturnValueCompletion<String> completion);
}

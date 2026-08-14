package org.zstack.storage.primary.block;

import org.zstack.header.core.ReturnValueCompletion;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/9 12:22
 */
public abstract class BackupStorageBlockKvmUploader {

    public abstract void uploadBits(String imageUuid, String bsPath, String psPath, ReturnValueCompletion<String> completion);

    public abstract void uploadBits(String imageUuid, String bsPath, String psPath, String hostUuid, ReturnValueCompletion<String> completion);

}

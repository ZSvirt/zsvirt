package org.zstack.storage.primary.block;

import org.zstack.header.core.Completion;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/9 12:24
 */
public abstract class BackupStorageBlockKvmDownloader {
    public abstract void downloadBits(String bsPath, String psPath, String hostUuid, Completion completion);
}

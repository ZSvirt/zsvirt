package org.zstack.storage.primary.sharedblock;

import org.zstack.header.core.Completion;

/**
 * Created by xing5 on 2016/3/27.
 */
public abstract class BackupStorageKvmDownloader {
    public abstract void downloadBits(String bsPath, String psPath, Completion completion);
}

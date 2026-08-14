package org.zstack.storage.primary.sharedblock;

import org.zstack.header.core.Completion;

public abstract class BackupStorageSharedBlockKvmDownloader {
    public abstract void downloadBits(String bsPath, String psPath, LvmlockdLockingType type, Completion completion);
}

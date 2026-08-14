package org.zstack.header.storage.primary;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.utils.DebugUtils;

/**
 * Created by mingjian.deng on 2017/11/3.
 */
public abstract class BackupStorageMediator {
    public BackupStorageInventory backupStorage;
    public MediatorParam param;

    public void checkParam() {
        DebugUtils.Assert(backupStorage != null, "backupStorage cannot be null");
        DebugUtils.Assert(param != null, "param cannot be null");
    }

    abstract public void download(ReturnValueCompletion<String> completion);

    abstract public void upload(ReturnValueCompletion<String> completion);

    abstract public boolean deleteWhenRollbackDownload();
}

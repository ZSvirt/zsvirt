package org.zstack.storage.primary.filesystem.nfs;

import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.storage.primary.filesystem.AbstractFileSystemHostHeartbeatChecker;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;

/**
 * @Author: DaoDao
 * @Date: 2023/3/8
 */
public class NFSHostHeartbeatChecker extends AbstractFileSystemHostHeartbeatChecker {
    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return PrimaryStorageType.valueOf(NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE);
    }

    @Override
    public int getWeight() {
        return 5;
    }
}

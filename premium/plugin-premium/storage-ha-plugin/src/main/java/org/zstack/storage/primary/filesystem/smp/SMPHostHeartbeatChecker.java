package org.zstack.storage.primary.filesystem.smp;

import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.storage.primary.filesystem.AbstractFileSystemHostHeartbeatChecker;
import org.zstack.storage.primary.smp.SMPConstants;

/**
 * @Author: DaoDao
 * @Date: 2023/3/8
 */
public class SMPHostHeartbeatChecker extends AbstractFileSystemHostHeartbeatChecker {
    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return PrimaryStorageType.valueOf(SMPConstants.SMP_TYPE);
    }

    @Override
    public int getWeight() {
        return 5;
    }
}

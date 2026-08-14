package org.zstack.header.storage.snapshot.group;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by LiangHanYu on 2022/6/9 18:30
 */
public interface MemorySnapshotValidatorExtensionPoint {
    default ErrorCode checkVmWhereMemorySnapshotExistExternalDevices(String VmInstanceUuid) {
        return null;
    }

    default ErrorCode checkL3IfReferencedByMemorySnapshot(String l3Uuid) {
        return null;
    }
}

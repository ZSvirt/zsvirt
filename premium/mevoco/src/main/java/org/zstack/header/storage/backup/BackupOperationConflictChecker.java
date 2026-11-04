package org.zstack.header.storage.backup;

import org.zstack.header.errorcode.ErrorCode;

public interface BackupOperationConflictChecker {
    ErrorCode check(String vmUuid, String currentResourceUuid);

    default ErrorCode checkChainSnapshot(String vmUuid) {
        return check(vmUuid, null);
    }
}

package org.zstack.sdk;

import org.zstack.sdk.ErrorCode;

public class CreateRootVolumeTemplateFromVolumeSnapshotFailure  {

    public java.lang.String backupStorageUuid;
    public void setBackupStorageUuid(java.lang.String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
    public java.lang.String getBackupStorageUuid() {
        return this.backupStorageUuid;
    }

    public ErrorCode error;
    public void setError(ErrorCode error) {
        this.error = error;
    }
    public ErrorCode getError() {
        return this.error;
    }

}

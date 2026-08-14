package org.zstack.header.storage.snapshot;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Create by weiwang at 2018-12-22
 */
public class BatchDeleteVolumeSnapshotStruct {
    private String snapshotUuid;
    private boolean success = true;
    private ErrorCode error;

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.success = false;
        this.error = error;
    }
}

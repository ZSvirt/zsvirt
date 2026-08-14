package org.zstack.sdk;

import org.zstack.sdk.ErrorCode;

public class DeleteSnapshotGroupResult  {

    public java.lang.String snapshotUuid;
    public void setSnapshotUuid(java.lang.String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }
    public java.lang.String getSnapshotUuid() {
        return this.snapshotUuid;
    }

    public java.lang.String volumeUuid;
    public void setVolumeUuid(java.lang.String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
    public java.lang.String getVolumeUuid() {
        return this.volumeUuid;
    }

    public boolean success;
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public boolean getSuccess() {
        return this.success;
    }

    public ErrorCode error;
    public void setError(ErrorCode error) {
        this.error = error;
    }
    public ErrorCode getError() {
        return this.error;
    }

}

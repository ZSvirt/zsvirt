package org.zstack.header.storage.snapshot.group;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by MaJin on 2019/7/10.
 */
public class RevertSnapshotGroupResult {
    private String snapshotUuid;
    private String volumeUuid;
    private boolean success = true;
    private ErrorCode error;

    public RevertSnapshotGroupResult(String snapshotUuid, String volumeUuid, ErrorCode error) {
        this.snapshotUuid = snapshotUuid;
        this.volumeUuid = volumeUuid;
        this.success = error == null;
        this.error = error;
    }

    public RevertSnapshotGroupResult() {

    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
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

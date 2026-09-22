package org.zstack.header.storage.snapshot;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Create by weiwang at 2018/6/11
 */
public class CreateVolumesSnapshotReply extends MessageReply {
    private List<VolumeSnapshotInventory> inventories;
    private List<String> hostBackupFileUuidList;
    private List<CreateVolumesSnapshotsJobStruct> failedSnapshotJobs;
    private ErrorCode partialError;

    public List<VolumeSnapshotInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotInventory> inventories) {
        this.inventories = inventories;
    }

    public List<String> getHostBackupFileUuidList() {
        return hostBackupFileUuidList;
    }

    public void setHostBackupFileUuidList(List<String> hostBackupFileUuidList) {
        this.hostBackupFileUuidList = hostBackupFileUuidList;
    }

    public List<CreateVolumesSnapshotsJobStruct> getFailedSnapshotJobs() {
        return failedSnapshotJobs;
    }

    public void setFailedSnapshotJobs(List<CreateVolumesSnapshotsJobStruct> failedSnapshotJobs) {
        this.failedSnapshotJobs = failedSnapshotJobs;
    }

    public ErrorCode getPartialError() {
        return partialError;
    }

    public void setPartialError(ErrorCode partialError) {
        this.partialError = partialError;
    }
}

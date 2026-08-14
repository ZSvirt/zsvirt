package org.zstack.header.storage.snapshot;

import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeMessage;

import java.util.List;

/**
 * Create by weiwang at 2018/6/11
 */
public class CreateVolumesSnapshotMsg extends NeedReplyMessage implements NeedQuotaCheckMessage, VolumeMessage {
    private String accountUuid;

    private ConsistentType consistentType = ConsistentType.Crash;

    private List<CreateVolumesSnapshotsJobStruct> volumeSnapshotJobs;

    private boolean backupHostFileIfNeeded;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public List<CreateVolumesSnapshotsJobStruct> getVolumeSnapshotJobs() {
        return volumeSnapshotJobs;
    }

    public void setVolumeSnapshotJobs(List<CreateVolumesSnapshotsJobStruct> volumeSnapshotJobs) {
        this.volumeSnapshotJobs = volumeSnapshotJobs;
    }

    @Override
    public String getVolumeUuid() {
        return volumeSnapshotJobs.get(0).getVolumeUuid();
    }

    public ConsistentType getConsistentType() {
        return consistentType;
    }

    public void setConsistentType(ConsistentType consistentType) {
        this.consistentType = consistentType;
    }

    public boolean isBackupHostFileIfNeeded() {
        return backupHostFileIfNeeded;
    }

    public void setBackupHostFileIfNeeded(boolean backupHostFileIfNeeded) {
        this.backupHostFileIfNeeded = backupHostFileIfNeeded;
    }
}

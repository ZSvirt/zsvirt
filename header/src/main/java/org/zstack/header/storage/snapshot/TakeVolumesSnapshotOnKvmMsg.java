package org.zstack.header.storage.snapshot;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.additions.VmHostFileBackupJob;

import java.util.List;

/**
 * Create by weiwang at 2018/6/12
 */
public class TakeVolumesSnapshotOnKvmMsg extends NeedReplyMessage implements HostMessage {
    private List<TakeSnapshotsOnKvmJobStruct> snapshotJobs;
    private String hostUuid;
    private List<VmHostFileBackupJob> vmHostFileBackupJobs;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<TakeSnapshotsOnKvmJobStruct> getSnapshotJobs() {
        return snapshotJobs;
    }

    public void setSnapshotJobs(List<TakeSnapshotsOnKvmJobStruct> snapshotJobs) {
        this.snapshotJobs = snapshotJobs;
    }

    public List<VmHostFileBackupJob> getVmHostFileBackupJobs() {
        return vmHostFileBackupJobs;
    }

    public void setVmHostFileBackupJobs(List<VmHostFileBackupJob> vmHostFileBackupJobs) {
        this.vmHostFileBackupJobs = vmHostFileBackupJobs;
    }

}

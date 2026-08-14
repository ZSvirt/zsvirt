package org.zstack.kvm.vmfiles.message;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.additions.VmHostFileBackupJob;

import java.util.List;

public class BackupVmHostFileOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private List<VmHostFileBackupJob> vmHostFileBackupJobs;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<VmHostFileBackupJob> getVmHostFileBackupJobs() {
        return vmHostFileBackupJobs;
    }

    public void setVmHostFileBackupJobs(List<VmHostFileBackupJob> vmHostFileBackupJobs) {
        this.vmHostFileBackupJobs = vmHostFileBackupJobs;
    }
}

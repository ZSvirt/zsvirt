package org.zstack.kvm.vmfiles.message;

import org.zstack.header.message.NeedReplyMessage;

public class SyncVmHostFilesFromHostMsg extends NeedReplyMessage {
    private String hostUuid;
    private String vmUuid;
    private String nvRamPath;
    private String tpmStateFolder;
    private String syncReason;
    private boolean syncToBackup;
    private String backupResourceUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getNvRamPath() {
        return nvRamPath;
    }

    public void setNvRamPath(String nvRamPath) {
        this.nvRamPath = nvRamPath;
    }

    public String getTpmStateFolder() {
        return tpmStateFolder;
    }

    public void setTpmStateFolder(String tpmStateFolder) {
        this.tpmStateFolder = tpmStateFolder;
    }

    public String getSyncReason() {
        return syncReason;
    }

    public void setSyncReason(String syncReason) {
        this.syncReason = syncReason;
    }

    public boolean isSyncToBackup() {
        return syncToBackup;
    }

    public void setSyncToBackup(boolean syncToBackup) {
        this.syncToBackup = syncToBackup;
    }

    public String getBackupResourceUuid() {
        return backupResourceUuid;
    }

    public void setBackupResourceUuid(String backupResourceUuid) {
        this.backupResourceUuid = backupResourceUuid;
    }
}

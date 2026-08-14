package org.zstack.sdk;



public class VmExternalBackupInfo extends org.zstack.sdk.ResourceExternalBackupInfo {

    public boolean liveBackup;
    public void setLiveBackup(boolean liveBackup) {
        this.liveBackup = liveBackup;
    }
    public boolean getLiveBackup() {
        return this.liveBackup;
    }

    public java.util.List volumes;
    public void setVolumes(java.util.List volumes) {
        this.volumes = volumes;
    }
    public java.util.List getVolumes() {
        return this.volumes;
    }

    public long totalSize;
    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }
    public long getTotalSize() {
        return this.totalSize;
    }

}

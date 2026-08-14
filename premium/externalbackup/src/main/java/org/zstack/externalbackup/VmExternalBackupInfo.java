package org.zstack.externalbackup;

import java.util.ArrayList;
import java.util.List;

public class VmExternalBackupInfo extends ResourceExternalBackupInfo {
    protected boolean liveBackup;
    protected List<VolumeExternalBackupInfo> volumes = new ArrayList<>();
    protected long totalSize;

    public void setLiveBackup(boolean liveBackup) {
        this.liveBackup = liveBackup;
    }

    public boolean isLiveBackup() {
        return liveBackup;
    }

    public List<VolumeExternalBackupInfo> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<VolumeExternalBackupInfo> volumes) {
        this.volumes = volumes;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public void refreshTotalSize() {
        totalSize = volumes.stream().mapToLong(it -> it.size).sum();
    }

    @Override
    public void setState(ResourceBackupState state) {
        super.setState(state);
        if (state == ResourceBackupState.Ready) {
            volumes.forEach(vol -> vol.setState(state));
        }
    }
}

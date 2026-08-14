package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 */
public class TakeSnapshotOnHypervisorReply extends MessageReply {
    private String newVolumeInstallPath;
    private String snapshotInstallPath;
    private long size;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getSnapshotInstallPath() {
        return snapshotInstallPath;
    }

    public void setSnapshotInstallPath(String snapshotInstallPath) {
        this.snapshotInstallPath = snapshotInstallPath;
    }

    public String getNewVolumeInstallPath() {
        return newVolumeInstallPath;
    }

    public void setNewVolumeInstallPath(String newVolumeInstallPath) {
        this.newVolumeInstallPath = newVolumeInstallPath;
    }
}

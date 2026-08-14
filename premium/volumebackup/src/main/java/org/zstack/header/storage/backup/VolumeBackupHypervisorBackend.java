package org.zstack.header.storage.backup;

public interface VolumeBackupHypervisorBackend {
    void handleMessage(VolumeBackupMessage msg);
}

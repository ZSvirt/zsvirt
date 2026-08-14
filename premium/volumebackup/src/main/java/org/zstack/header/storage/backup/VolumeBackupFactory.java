package org.zstack.header.storage.backup;

public interface VolumeBackupFactory {
    String getHypervisorType();

    VolumeBackupHypervisorBackend getHypervisorBackend();
}

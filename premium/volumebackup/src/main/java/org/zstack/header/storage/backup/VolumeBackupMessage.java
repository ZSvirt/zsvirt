package org.zstack.header.storage.backup;

public interface VolumeBackupMessage {
    String getVolumeUuid();
    String getBackupStorageUuid();
}

package org.zstack.header.volume;

public interface VolumeBackupEncryptionConversionCommitter {
    boolean isAvailable();

    void commitVolumeBackupEncryptionConversion(VolumeBackupEncryptionConversionResult result);
}

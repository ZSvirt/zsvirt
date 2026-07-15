package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.volume.VolumeStats;

public interface ZbsVolumeEncryptionBackend {
    String getPrimaryStorageUuid();

    String buildConfiguredVolumePath(String volumeName);

    String buildEncryptedTargetPath(String installPath);

    void createLuksBackingVolume(String installPath, long virtualSize, ReturnValueCompletion<String> completion);

    void deleteLuksBackingVolume(String installPath);

    void cloneVolumeAsBacking(String srcInstallPath, CreateVolumeSpec dst, ReturnValueCompletion<VolumeStats> completion);

    void resolveSnapshotPathForQemu(String installPath, ReturnValueCompletion<String> completion);

    void checkNoSnapshots(String installPath, Completion completion);

    void validateConversionPaths(String sourceInstallPath, String targetInstallPath);

    void createConversionTarget(String targetInstallPath, long virtualSize, boolean targetEncrypted,
                                ReturnValueCompletion<String> completion);

    void deleteConversionTarget(String targetInstallPath, Completion completion);

    void stats(String installPath, ReturnValueCompletion<VolumeStats> completion);
}

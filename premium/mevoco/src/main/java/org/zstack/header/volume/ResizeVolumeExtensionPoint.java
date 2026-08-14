package org.zstack.header.volume;

import org.zstack.header.core.Completion;

public interface ResizeVolumeExtensionPoint {
    void beforeResizeVolume(VolumeVO volume, long resize, VolumeType type, ResizeVolumeStruct struct, Completion completion);

    default boolean snapshotBeforeResizeVolume(String volumeInstallPath, VolumeType type) {
        return true;
    }
}

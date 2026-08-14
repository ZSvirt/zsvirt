package org.zstack.header.volume;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by frank on 6/18/2015.
 */
public interface VolumeDeletionExtensionPoint {
    void preDeleteVolume(VolumeInventory volume);

    void beforeDeleteVolume(VolumeInventory volume);

    void afterDeleteVolume(VolumeInventory volume, Completion completion);

    void failedToDeleteVolume(VolumeInventory volume, ErrorCode errorCode);
}

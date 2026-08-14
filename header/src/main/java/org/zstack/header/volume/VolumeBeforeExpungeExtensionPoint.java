package org.zstack.header.volume;

import org.zstack.header.core.Completion;

/**
 * Created by xing5 on 2016/5/3.
 */
public interface VolumeBeforeExpungeExtensionPoint {
    void volumePreExpunge(VolumeInventory volume);
    void volumeBeforeExpunge(VolumeInventory volume, Completion completion);

    default boolean skipExpungeVolume(VolumeInventory volume) {
        return false;
    }
}

package org.zstack.header.volume;

/**
 * Created by xing5 on 2016/5/3.
 */
public interface VolumeAfterExpungeExtensionPoint {
    void volumeAfterExpunge(VolumeInventory volume);
}

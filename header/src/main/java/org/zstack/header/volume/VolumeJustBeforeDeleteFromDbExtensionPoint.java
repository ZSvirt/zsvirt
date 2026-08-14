package org.zstack.header.volume;

/**
 * Created by kayo on 2018/8/16.
 */
public interface VolumeJustBeforeDeleteFromDbExtensionPoint {
    void volumeJustBeforeDeleteFromDb(VolumeInventory inv);
}

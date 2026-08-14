package org.zstack.header.volume;

public interface FlattenVolumeExtensionPoint {
    void afterFlattenVolume(VolumeInventory volume);
}

package org.zstack.header.volume;

/**
 * Created by frank on 11/24/2015.
 */
public interface RecoverDataVolumeExtensionPoint {
    void preRecoverDataVolume(VolumeInventory vol);

    void beforeRecoverDataVolume(VolumeInventory vol);

    void afterRecoverDataVolume(VolumeInventory vol);
}

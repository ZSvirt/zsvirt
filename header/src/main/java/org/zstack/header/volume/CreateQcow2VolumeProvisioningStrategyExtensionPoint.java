package org.zstack.header.volume;

public interface CreateQcow2VolumeProvisioningStrategyExtensionPoint {
    void saveQcow2VolumeProvisioningStrategy(VolumeInventory volume, boolean hasBackingFile);
}

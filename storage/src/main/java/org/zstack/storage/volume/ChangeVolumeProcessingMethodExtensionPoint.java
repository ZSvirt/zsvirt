package org.zstack.storage.volume;

import org.zstack.header.volume.VolumeDeletionPolicyManager;
import org.zstack.header.volume.VolumeInventory;

public interface ChangeVolumeProcessingMethodExtensionPoint {
    VolumeDeletionPolicyManager.VolumeDeletionPolicy getTransientVolumeDeletionPolicy(VolumeInventory transientVolume);
}

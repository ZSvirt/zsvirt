package org.zstack.header.volume;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
/**
 * Created by MaJin on 2019/4/2.
 */
public interface OverwriteVolumeExtensionPoint {
    void innerOverwriteVolume(VolumeInventory originVolume, VolumeInventory transientVolume, VolumeDeletionPolicy originVolumeDeletionPolicy);
    void afterOverwriteVolume(VolumeInventory volume, VolumeInventory transientVolume);
}

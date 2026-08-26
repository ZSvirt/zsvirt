package org.zstack.header.volume;

/**
 * Created by mingjian.deng on 2017/9/20.
 */
public interface CreateDataVolumeExtensionPoint {
    void preCreateVolume(VolumeCreateMessage msg);

    void beforeCreateVolume(VolumeInventory volume);

    void afterCreateVolume(VolumeVO volume);

    /**
     * Called when a data volume is created from a volume snapshot. Extensions that need
     * snapshot context, for example inheriting encryption key bindings from an encrypted
     * snapshot to the new volume, can override this method.
     */
    default void afterCreateVolume(VolumeVO volume, String snapshotUuid) {
        afterCreateVolume(volume);
    }
}

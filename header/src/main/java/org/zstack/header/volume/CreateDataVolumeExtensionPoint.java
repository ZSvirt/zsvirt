package org.zstack.header.volume;

/**
 * Created by mingjian.deng on 2017/9/20.
 */
public interface CreateDataVolumeExtensionPoint {
    void preCreateVolume(VolumeCreateMessage msg);

    void beforeCreateVolume(VolumeInventory volume);

    void afterCreateVolume(VolumeVO volume);
}

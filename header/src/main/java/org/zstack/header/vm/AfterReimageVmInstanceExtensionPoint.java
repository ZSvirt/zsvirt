package org.zstack.header.vm;

import org.zstack.header.volume.VolumeInventory;

/**
 * Created by miao on 1/5/17.
 */
public interface AfterReimageVmInstanceExtensionPoint {
    void afterReimageVmInstance(VolumeInventory inventory);
}

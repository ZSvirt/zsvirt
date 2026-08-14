package org.zstack.header.vm;

import org.zstack.header.volume.VolumeInventory;

/**
 * Created by LiangHanYu on 2022/9/26 17:49
 */
public class ArchiveVolumeBundle extends ArchiveBundle {
    VolumeInventory volumeInventory;

    public ArchiveVolumeBundle() {
    }

    public ArchiveVolumeBundle(VolumeInventory volumeInventory) {
        this.volumeInventory = volumeInventory;
    }

    public VolumeInventory getVolumeInventory() {
        return volumeInventory;
    }

    public void setVolumeInventory(VolumeInventory volumeInventory) {
        this.volumeInventory = volumeInventory;
    }
}

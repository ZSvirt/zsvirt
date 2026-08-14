package org.zstack.kvm;

import org.zstack.header.volume.VolumeInventory;

public interface KVMConvertVolumeExtensionPoint {
    VolumeTO convertVolumeIfNeed(KVMHostInventory host, VolumeInventory inventory, VolumeTO to);
}

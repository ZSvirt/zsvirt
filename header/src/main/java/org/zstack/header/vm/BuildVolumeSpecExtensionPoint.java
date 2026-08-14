package org.zstack.header.vm;

import org.zstack.header.volume.VolumeInventory;

import java.util.List;

/**
 * Created by miao on 1/12/17.
 */
public interface BuildVolumeSpecExtensionPoint {
    List<VolumeInventory> supplyAdditionalVolumesForVmInstance(String VmInstanceUuid);
}

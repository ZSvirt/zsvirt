package org.zstack.header.vm;

import org.zstack.header.volume.VolumeVO;

import java.util.List;

/**
 * Created by frank on 7/16/2015.
 */
public interface GetAttachableVolumeExtensionPoint {
    List<VolumeVO> returnAttachableVolumes(VmInstanceInventory vm, List<VolumeVO> candidates);
}

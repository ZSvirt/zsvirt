package org.zstack.header.volume;

import org.zstack.header.vm.VmInstanceVO;

import java.util.List;

/**
 * Created by frank on 11/19/2015.
 */
public interface VolumeGetAttachableVmExtensionPoint {
    List<VmInstanceVO> returnAttachableVms(VolumeInventory vol, List<VmInstanceVO> candidates);
}

package org.zstack.header.storageDevice;

import org.zstack.header.vm.VmInstanceInventory;

import java.util.List;

public interface GetScsiLunCandidatesExtensionPoint {
    List<ScsiLunVO> filterGetScsiLunCandidates(VmInstanceInventory vm, List<ScsiLunVO> scsiLuns);
}

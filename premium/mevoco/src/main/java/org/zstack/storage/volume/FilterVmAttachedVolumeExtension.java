package org.zstack.storage.volume;

import org.zstack.core.db.Q;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeGetAttachableVmExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO_;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by camile on 2017/8/17.
 */
public class FilterVmAttachedVolumeExtension implements VolumeGetAttachableVmExtensionPoint {
    @Override
    public List<VmInstanceVO> returnAttachableVms(VolumeInventory vol, List<VmInstanceVO> candidates) {
        if (!vol.isShareable()) {
            return candidates;
        }

        List<String> candidateVmUuids = candidates.stream()
                .map(VmInstanceVO::getUuid)
                .collect(Collectors.toList());

        List<String> attachedVmUuids = Q.New(ShareableVolumeVmInstanceRefVO.class)
                .select(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid)
                .in(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, candidateVmUuids)
                .eq(ShareableVolumeVmInstanceRefVO_.volumeUuid, vol.getUuid())
                .listValues();

        if (attachedVmUuids.isEmpty()) {
            return candidates;
        }

        List<VmInstanceVO> newCandidates = candidates.stream()
                .filter(vo -> !attachedVmUuids.contains(vo.getUuid()))
                .collect(Collectors.toList());

        return newCandidates;
    }
}

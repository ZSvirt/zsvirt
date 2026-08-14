package org.zstack.mevoco;

import org.zstack.core.db.Q;
import org.zstack.header.volume.VolumeAttachedJudger;
import org.zstack.header.volume.VolumeInventory;

import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/5/30.
 */
public class ShareableVolumeAttachedJudger implements VolumeAttachedJudger {
    @Override
    public Boolean isAttached(VolumeInventory inv) {
        return inv.isShareable() && Q.New(ShareableVolumeVmInstanceRefVO.class)
                .eq(ShareableVolumeVmInstanceRefVO_.volumeUuid, inv.getUuid())
                .isExists();
    }

    @Override
    public List<String> getAttachedVmUuids(VolumeInventory inv) {
        return inv.isShareable() ? Q.New(ShareableVolumeVmInstanceRefVO.class)
                .select(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid)
                .eq(ShareableVolumeVmInstanceRefVO_.volumeUuid, inv.getUuid())
                .listValues() : Collections.emptyList();
    }
}

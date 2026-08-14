package org.zstack.storage.volume;

import org.zstack.header.volume.VolumeAttachedJudger;
import org.zstack.header.volume.VolumeInventory;

import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/5/30.
 */
public class DefaultVolumeAttachedJudger implements VolumeAttachedJudger {
    @Override
    public Boolean isAttached(VolumeInventory inv) {
        return inv.getVmInstanceUuid() != null;
    }

    @Override
    public List<String> getAttachedVmUuids(VolumeInventory inv) {
        return inv.getVmInstanceUuid() == null ? Collections.emptyList() : Collections.singletonList(inv.getVmInstanceUuid());
    }
}

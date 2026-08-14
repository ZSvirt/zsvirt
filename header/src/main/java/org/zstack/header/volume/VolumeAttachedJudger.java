package org.zstack.header.volume;

import org.zstack.header.rest.NoSDK;

import java.util.List;

/**
 * Created by MaJin on 2019/5/30.
 */
@NoSDK
public interface VolumeAttachedJudger {
    Boolean isAttached(VolumeInventory inv);

    List<String> getAttachedVmUuids(VolumeInventory inv);
}

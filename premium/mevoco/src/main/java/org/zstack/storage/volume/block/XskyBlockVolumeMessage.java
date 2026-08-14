package org.zstack.storage.volume.block;

import org.zstack.header.message.APIParam;

public interface XskyBlockVolumeMessage extends BlockVolumeMessage {
    Long getBurstTotalBw();
    Long getBurstTotalIops();
    Long getMaxTotalBw();
    Long getMaxTotalIops();
}

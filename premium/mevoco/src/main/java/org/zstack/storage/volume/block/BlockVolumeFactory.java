package org.zstack.storage.volume.block;

import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.block.APICreateBlockVolumeMsg;
import org.zstack.header.volume.block.BlockVolumeVO;

public interface BlockVolumeFactory {
    String getType();

    BlockVolumeVO createBlockVolume(VolumeVO vo, APICreateBlockVolumeMsg msg);

    BlockVolumeBase getBlockVolume(BlockVolumeVO vo);

    void validate(String volumeName, String protocol, String primaryStorageUuid);
}

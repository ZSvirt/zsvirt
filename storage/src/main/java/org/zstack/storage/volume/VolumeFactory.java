package org.zstack.storage.volume;

import org.zstack.header.volume.VolumeVO;

/**
 * Created by miao on 12/23/16.
 */
public interface VolumeFactory {
    VolumeBase makeVolumeBase(VolumeVO vo);
}

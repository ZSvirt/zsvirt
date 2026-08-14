package org.zstack.cbd.kvm;

import org.zstack.header.storage.addon.primary.ActiveVolumeTO;
import org.zstack.header.volume.VolumeProtocol;

/**
 * @author Xingwei Yu
 * @date 2024/5/17 16:36
 */
public class CbdVolumeTo extends ActiveVolumeTO {
    {
        protocol = VolumeProtocol.CBD.name();
    }
}

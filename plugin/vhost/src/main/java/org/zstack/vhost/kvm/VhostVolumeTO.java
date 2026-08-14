package org.zstack.vhost.kvm;

import org.zstack.header.storage.addon.primary.ActiveVolumeTO;
import org.zstack.header.volume.VolumeProtocol;

public class VhostVolumeTO extends ActiveVolumeTO {
    {
        protocol = VolumeProtocol.Vhost.name();
    }
}

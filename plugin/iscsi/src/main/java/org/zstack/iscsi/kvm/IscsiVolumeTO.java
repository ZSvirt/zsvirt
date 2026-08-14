package org.zstack.iscsi.kvm;

import org.zstack.header.storage.addon.primary.ActiveVolumeTO;
import org.zstack.header.volume.VolumeProtocol;

public class IscsiVolumeTO extends ActiveVolumeTO {
    {
        protocol = VolumeProtocol.iSCSI.name();
    }
}

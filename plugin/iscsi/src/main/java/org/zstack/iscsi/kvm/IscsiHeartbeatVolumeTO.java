package org.zstack.iscsi.kvm;

import org.zstack.header.storage.addon.primary.HeartbeatVolumeTO;

public class IscsiHeartbeatVolumeTO extends HeartbeatVolumeTO {
    {
        protocol = "iscsi";
    }
}

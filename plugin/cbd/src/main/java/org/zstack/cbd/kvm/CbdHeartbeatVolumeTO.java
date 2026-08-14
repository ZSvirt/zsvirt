package org.zstack.cbd.kvm;

import org.zstack.header.storage.addon.primary.HeartbeatVolumeTO;

/**
 * @author Xingwei Yu
 * @date 2024/4/10 10:42
 */
public class CbdHeartbeatVolumeTO extends HeartbeatVolumeTO {
    {
        protocol = "cbd";
    }
}

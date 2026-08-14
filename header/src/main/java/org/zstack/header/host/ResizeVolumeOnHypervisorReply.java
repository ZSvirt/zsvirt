package org.zstack.header.host;

import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by kayo on 2018/4/2.
 */
public class ResizeVolumeOnHypervisorReply extends MessageReply {
    private VolumeInventory volume;

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }
}

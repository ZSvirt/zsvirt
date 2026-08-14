package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2019/4/2.
 */
public class OverwriteVolumeMsg extends NeedReplyMessage implements VolumeMessage {
    private VolumeInventory originVolume;
    private VolumeInventory transientVolume;

    @Override
    public String getVolumeUuid() {
        return transientVolume.getUuid();
    }
    public VolumeInventory getTransientVolume() {
        return transientVolume;
    }

    public void setTransientVolume(VolumeInventory transientVolume) {
        this.transientVolume = transientVolume;
    }

    public void setOriginVolume(VolumeInventory originVolume) {
        this.originVolume = originVolume;
    }

    public VolumeInventory getOriginVolume() {
        return originVolume;
    }
}

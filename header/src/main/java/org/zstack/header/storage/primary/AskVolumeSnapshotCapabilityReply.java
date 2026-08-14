package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Created by frank on 6/9/2015.
 */
public class AskVolumeSnapshotCapabilityReply extends MessageReply {
    private VolumeSnapshotCapability capability;

    public VolumeSnapshotCapability getCapability() {
        return capability;
    }

    public void setCapability(VolumeSnapshotCapability capability) {
        this.capability = capability;
    }
}

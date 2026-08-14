package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

/**
 */
public class AskVolumeSnapshotStructReply extends MessageReply {
    private VolumeSnapshotStruct struct;

    public VolumeSnapshotStruct getStruct() {
        return struct;
    }

    public void setStruct(VolumeSnapshotStruct struct) {
        this.struct = struct;
    }
}

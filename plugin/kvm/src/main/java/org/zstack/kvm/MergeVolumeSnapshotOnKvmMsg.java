package org.zstack.kvm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;

/**
 */
public class MergeVolumeSnapshotOnKvmMsg extends NeedReplyMessage implements HostMessage {
    private VolumeSnapshotInventory from;
    private VolumeInventory to;
    private String hostUuid;
    private boolean fullRebase;

    public boolean isFullRebase() {
        return fullRebase || from == null;
    }

    public void setFullRebase(boolean fullRebase) {
        this.fullRebase = fullRebase;
    }

    public VolumeSnapshotInventory getFrom() {
        return from;
    }

    public void setFrom(VolumeSnapshotInventory from) {
        this.from = from;
    }

    public VolumeInventory getTo() {
        return to;
    }

    public void setTo(VolumeInventory to) {
        this.to = to;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}

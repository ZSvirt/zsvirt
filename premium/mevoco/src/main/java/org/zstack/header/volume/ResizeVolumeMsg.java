package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

public class ResizeVolumeMsg extends NeedReplyMessage {
    private String volumeUuid;
    private Boolean takeSnapshot;  // don't use 'boolean'.
    private boolean force;
    private long size;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public Boolean getTakeSnapshot() {
        return takeSnapshot;
    }

    public void setTakeSnapshot(Boolean takeSnapshot) {
        this.takeSnapshot = takeSnapshot;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}

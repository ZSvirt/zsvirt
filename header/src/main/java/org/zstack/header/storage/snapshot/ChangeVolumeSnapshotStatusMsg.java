package org.zstack.header.storage.snapshot;

import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class ChangeVolumeSnapshotStatusMsg extends NeedReplyMessage implements VolumeSnapshotMessage {
    private String event;
    private String snapshotUuid;

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    @Override
    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    @Override
    public String getVolumeUuid() {
        return null;
    }

    @Override
    public void setVolumeUuid(String treeUuid) {
    }

    @Override
    public void setTreeUuid(String treeUuid) {

    }

    @Override
    public String getTreeUuid() {
        return null;
    }
}

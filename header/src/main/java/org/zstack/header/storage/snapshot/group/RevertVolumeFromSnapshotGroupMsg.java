package org.zstack.header.storage.snapshot.group;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.RevertVolumeSnapshotMessage;

/**
 * Created by MaJin on 2019/7/10.
 */
public class RevertVolumeFromSnapshotGroupMsg extends NeedReplyMessage implements RevertVolumeSnapshotMessage {
    private String snapshotUuid;
    private String volumeUuid;
    private String treeUuid;
    private SessionInventory session;
    private String newSnapshotGroupUuid;

    @Override
    public SessionInventory getSession() {
        return session;
    }

    @Override
    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    @Override
    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    @Override
    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }

    @Override
    public String getTreeUuid() {
        return treeUuid;
    }

    public String getNewSnapshotGroupUuid() {
        return newSnapshotGroupUuid;
    }

    public void setNewSnapshotGroupUuid(String newSnapshotGroupUuid) {
        this.newSnapshotGroupUuid = newSnapshotGroupUuid;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }
}

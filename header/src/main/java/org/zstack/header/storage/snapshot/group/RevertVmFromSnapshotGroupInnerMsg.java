package org.zstack.header.storage.snapshot.group;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.SnapshotBackendOperation;

/**
 * Created by MaJin on 2019/7/10.
 */
public class RevertVmFromSnapshotGroupInnerMsg extends NeedReplyMessage implements VolumeSnapshotGroupMessage {
    private String uuid;
    private SessionInventory session;

    @Override
    public String getGroupUuid() {
        return uuid;
    }

    @Override
    public SnapshotBackendOperation getBackendOperation() {
        return SnapshotBackendOperation.FILE_CREATION;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }
}

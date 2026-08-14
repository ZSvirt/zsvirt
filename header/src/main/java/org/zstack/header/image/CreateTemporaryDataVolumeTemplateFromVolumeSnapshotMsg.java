package org.zstack.header.image;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by MaJin on 2021/3/18.
 */
public class CreateTemporaryDataVolumeTemplateFromVolumeSnapshotMsg extends NeedReplyMessage implements CreateTemporaryDataVolumeTemplateMessage {
    private String snapshotUuid;
    private SessionInventory session;

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    @Override
    public String getName() {
        return "temporary-image-from-volume-snapshot-" + snapshotUuid;
    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    @Override
    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }
}

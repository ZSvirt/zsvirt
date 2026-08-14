package org.zstack.header.storage.snapshot;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2019/8/8.
 */
public class GetVolumeSnapshotTreeRootNodeMsg extends NeedReplyMessage {
    private String volumeUuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}

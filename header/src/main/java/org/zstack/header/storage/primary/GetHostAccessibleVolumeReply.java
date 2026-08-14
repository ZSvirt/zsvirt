package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by MaJin on 2019/11/29.
 */
public class GetHostAccessibleVolumeReply extends MessageReply {
    private List<String> volumeUuids;

    public List<String> getVolumeUuids() {
        return volumeUuids;
    }

    public void setVolumeUuids(List<String> volumeUuids) {
        this.volumeUuids = volumeUuids;
    }
}

package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class GetVolumeLocalTaskMsg extends NeedReplyMessage {
    List<String> volumeUuids;

    public void setVolumeUuids(List<String> volumeUuids) {
        this.volumeUuids = volumeUuids;
    }

    public List<String> getVolumeUuids() {
        return volumeUuids;
    }
}

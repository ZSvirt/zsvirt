package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 11/13/2015.
 */
public class ExpungeVolumeMsg extends NeedReplyMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}

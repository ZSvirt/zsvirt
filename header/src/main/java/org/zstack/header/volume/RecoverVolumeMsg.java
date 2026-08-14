package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 1/28/2016.
 */
public class RecoverVolumeMsg extends NeedReplyMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}

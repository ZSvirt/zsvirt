package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by xing5 on 2016/4/27.
 */
public class SyncVolumeSizeMsg extends NeedReplyMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}

package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2019/8/8.
 */
public class GetVolumeBackingInstallPathMsg extends NeedReplyMessage implements VolumeMessage {
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}

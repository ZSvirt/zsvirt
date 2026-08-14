package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by kayo on 2018/3/9.
 */
public class ChangeVolumeStatusMsg extends NeedReplyMessage implements VolumeMessage {
    private String volumeUuid;
    private VolumeStatus status;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public VolumeStatus getStatus() {
        return status;
    }

    public void setStatus(VolumeStatus status) {
        this.status = status;
    }
}

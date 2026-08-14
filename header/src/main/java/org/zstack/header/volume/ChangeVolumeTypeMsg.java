package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

public class ChangeVolumeTypeMsg extends NeedReplyMessage implements VolumeMessage {
    private String uuid;
    private VolumeType type;

    @Override
    public String getVolumeUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public VolumeType getType() {
        return type;
    }

    public void setType(VolumeType type) {
        this.type = type;
    }
}

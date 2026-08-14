package org.zstack.header.volume;

import org.zstack.header.message.CancelMessage;

public class CancelFlattenVolumeMsg extends CancelMessage implements VolumeMessage {
    private String uuid;

    @Override
    public String getVolumeUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
}

package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

public class FlattenVolumeMsg extends NeedReplyMessage implements VolumeMessage {
    private String uuid;
    private boolean dryRun;

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

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isDryRun() {
        return dryRun;
    }
}

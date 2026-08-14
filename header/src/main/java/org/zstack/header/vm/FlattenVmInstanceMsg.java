package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

public class FlattenVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String uuid;
    private boolean dryRun;
    private boolean full;

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

    public void setFull(boolean full) {
        this.full = full;
    }

    public boolean isFull() {
        return full;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }
}

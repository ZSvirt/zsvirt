package org.zstack.header.vm;

import org.zstack.header.message.CancelMessage;
import org.zstack.header.message.NeedReplyMessage;

public class CancelFlattenVmInstanceMsg extends CancelMessage implements VmInstanceMessage {
    private String uuid;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
}

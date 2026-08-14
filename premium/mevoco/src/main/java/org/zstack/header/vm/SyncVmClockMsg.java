package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by Wenhao.Zhang on 22/06/15
 */
public class SyncVmClockMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }
}

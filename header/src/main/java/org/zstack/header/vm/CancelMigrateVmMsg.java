package org.zstack.header.vm;

import org.zstack.header.message.CancelMessage;

/**
 * Created by MaJin on 2020/1/3.
 */
public class CancelMigrateVmMsg extends CancelMessage implements VmInstanceMessage {
    private String uuid;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

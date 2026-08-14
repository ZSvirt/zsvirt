package org.zstack.header.baremetal.instance;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 7/6/18.
 */
public class StopBaremetalInstanceMsg extends NeedReplyMessage implements BaremetalInstanceMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getBaremetalInstanceUuid() {
        return getUuid();
    }
}

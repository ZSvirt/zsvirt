package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2018-10-12.
 */
public class StartBaremetalPxeServerMsg extends NeedReplyMessage implements BaremetalPxeServerMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getPxeServerUuid() {
        return uuid;
    }
}

package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 6/24/17.
 */
public class DeleteBaremetalChassisMsg extends NeedReplyMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

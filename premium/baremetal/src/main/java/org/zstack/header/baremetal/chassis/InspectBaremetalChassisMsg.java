package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2018-10-30.
 */
public class InspectBaremetalChassisMsg extends NeedReplyMessage implements BaremetalChassisMessage {
    private String uuid;

    @Override
    public String getBaremetalChassisUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

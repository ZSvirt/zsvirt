package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2018-10-08.
 */
public class BatchCreateBaremetalChassisMsg extends NeedReplyMessage {
    private String baremetalChassisInfo;

    public String getBaremetalChassisInfo() {
        return baremetalChassisInfo;
    }

    public void setBaremetalChassisInfo(String baremetalChassisInfo) {
        this.baremetalChassisInfo = baremetalChassisInfo;
    }
}

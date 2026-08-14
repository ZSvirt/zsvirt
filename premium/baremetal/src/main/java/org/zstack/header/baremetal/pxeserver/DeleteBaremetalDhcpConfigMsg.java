package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2019-01-21.
 */
public class DeleteBaremetalDhcpConfigMsg extends NeedReplyMessage implements BaremetalPxeServerMessage {
    private String pxeServerUuid;
    private String chassisUuid;

    public String getPxeServerUuid() {
        return pxeServerUuid;
    }

    public void setPxeServerUuid(String pxeServerUuid) {
        this.pxeServerUuid = pxeServerUuid;
    }

    public String getChassisUuid() {
        return chassisUuid;
    }

    public void setChassisUuid(String chassisUuid) {
        this.chassisUuid = chassisUuid;
    }
}

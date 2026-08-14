package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2019-01-21.
 */
public class CreateBaremetalDhcpConfigMsg extends NeedReplyMessage implements BaremetalPxeServerMessage {
    private String pxeServerUuid;
    private String chassisUuid;
    private String pxeNicMac;
    private String pxeNicIp;

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

    public String getPxeNicMac() {
        return pxeNicMac;
    }

    public void setPxeNicMac(String pxeNicMac) {
        this.pxeNicMac = pxeNicMac;
    }

    public String getPxeNicIp() {
        return pxeNicIp;
    }

    public void setPxeNicIp(String pxeNicIp) {
        this.pxeNicIp = pxeNicIp;
    }
}

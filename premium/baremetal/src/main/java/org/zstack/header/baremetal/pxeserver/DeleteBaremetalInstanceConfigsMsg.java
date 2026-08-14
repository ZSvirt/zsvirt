package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2018-10-12.
 */
public class DeleteBaremetalInstanceConfigsMsg extends NeedReplyMessage implements BaremetalPxeServerMessage {
    private String pxeServerUuid;
    private String pxeNicMac;

    @Override
    public String getPxeServerUuid() {
        return pxeServerUuid;
    }

    public void setPxeServerUuid(String pxeServerUuid) {
        this.pxeServerUuid = pxeServerUuid;
    }

    public String getPxeNicMac() {
        return pxeNicMac;
    }

    public void setPxeNicMac(String pxeNicMac) {
        this.pxeNicMac = pxeNicMac;
    }
}

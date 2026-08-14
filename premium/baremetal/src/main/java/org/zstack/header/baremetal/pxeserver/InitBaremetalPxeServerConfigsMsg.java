package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2018-10-12.
 */
public class InitBaremetalPxeServerConfigsMsg extends NeedReplyMessage implements BaremetalPxeServerMessage {
    private String pxeServerUuid;

    @Override
    public String getPxeServerUuid() {
        return pxeServerUuid;
    }

    public void setPxeServerUuid(String pxeServerUuid) {
        this.pxeServerUuid = pxeServerUuid;
    }
}

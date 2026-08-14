package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 8/2/17.
 */
public class CreateBaremetalNoVNCProxyMsg extends NeedReplyMessage implements BaremetalPxeServerMessage {
    private String pxeServerUuid;
    private String baremetalInstanceUuid;
    private String upstream;

    @Override
    public String getPxeServerUuid() {
        return pxeServerUuid;
    }

    public void setPxeServerUuid(String pxeServerUuid) {
        this.pxeServerUuid = pxeServerUuid;
    }

    public String getBaremetalInstanceUuid() {
        return baremetalInstanceUuid;
    }

    public void setBaremetalInstanceUuid(String baremetalInstanceUuid) {
        this.baremetalInstanceUuid = baremetalInstanceUuid;
    }

    public String getUpstream() {
        return upstream;
    }

    public void setUpstream(String upstream) {
        this.upstream = upstream;
    }
}

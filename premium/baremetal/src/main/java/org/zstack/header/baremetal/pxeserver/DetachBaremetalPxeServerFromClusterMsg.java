package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2019-01-23.
 */
public class DetachBaremetalPxeServerFromClusterMsg extends NeedReplyMessage implements BaremetalPxeServerMessage {
    private String pxeServerUuid;
    private String clusterUuid;

    @Override
    public String getPxeServerUuid() {
        return pxeServerUuid;
    }

    public void setPxeServerUuid(String pxeServerUuid) {
        this.pxeServerUuid = pxeServerUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
}

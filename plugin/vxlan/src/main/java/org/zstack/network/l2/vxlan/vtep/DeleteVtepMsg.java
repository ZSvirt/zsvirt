package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.L2NetworkMessage;

/**
 * Created by weiwang on 12/05/2017.
 */
public class DeleteVtepMsg extends NeedReplyMessage implements L2NetworkMessage {
    private String l2NetworkUuid;

    private String vtepUuid;

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getVtepUuid() {
        return vtepUuid;
    }

    public void setVtepUuid(String vtepUuid) {
        this.vtepUuid = vtepUuid;
    }
}

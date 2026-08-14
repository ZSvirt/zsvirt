package org.zstack.header.network.l2;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.DeletionMessage;

/**
 * Created by weiwang on 21/03/2017.
 */
public class DeleteL2NetworkMsg extends DeletionMessage implements L2NetworkMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getL2NetworkUuid() {
        return uuid;
    }
}

package org.zstack.network.service.eip;

import org.zstack.header.message.DeletionMessage;

public class EipDeletionMsg extends DeletionMessage implements EipMessage {
    private String eipUuid;

    @Override
    public String getEipUuid() {
        return eipUuid;
    }

    public void setEipUuid(String eipUuid) {
        this.eipUuid = eipUuid;
    }
}

package org.zstack.crypto.keyprovider.api;

import org.zstack.crypto.keyprovider.KeyProviderMessage;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;

public abstract class APIDeleteKeyProviderMsg extends APIDeleteMessage implements KeyProviderMessage {
    @APIParam(resourceType = KeyProviderVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getKeyProviderUuid() {
        return uuid;
    }

}

package org.zstack.crypto.keyprovider.api;

import org.zstack.crypto.keyprovider.KeyProviderMessage;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

public abstract class APIUpdateKeyProviderMsg extends APIMessage implements KeyProviderMessage {
    @APIParam(resourceType = KeyProviderVO.class, emptyString = false)
    private String uuid;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getKeyProviderUuid() {
        return uuid;
    }
}

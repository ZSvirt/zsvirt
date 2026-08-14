package org.zstack.crypto.keyprovider.api;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;

public abstract class APICreateKeyProviderMsg extends APICreateMessage {
    @APIParam(maxLength = 255, emptyString = false)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APINoSee
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}

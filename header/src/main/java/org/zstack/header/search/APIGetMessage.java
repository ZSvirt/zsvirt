package org.zstack.header.search;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

@Deprecated
public abstract class APIGetMessage extends APIMessage {
    @APIParam
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

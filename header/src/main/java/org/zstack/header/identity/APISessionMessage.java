package org.zstack.header.identity;

import org.zstack.header.message.APISyncCallMessage;

public abstract class APISessionMessage extends APISyncCallMessage {
    public abstract String getUsername();
    public abstract String getPassword();
    public abstract String getLoginType();
}

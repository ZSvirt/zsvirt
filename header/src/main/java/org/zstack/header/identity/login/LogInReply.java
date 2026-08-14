package org.zstack.header.identity.login;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.MessageReply;

public class LogInReply extends MessageReply {
    private SessionInventory session;

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }
}

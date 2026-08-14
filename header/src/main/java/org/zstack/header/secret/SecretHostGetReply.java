package org.zstack.header.secret;

import org.zstack.header.message.MessageReply;

/** Reply for SecretHostGetMsg. */
public class SecretHostGetReply extends MessageReply {
    public static final String ERROR_CODE_SECRET_NOT_FOUND = "KEY_AGENT_SECRET_NOT_FOUND";

    private String secretUuid;

    public String getSecretUuid() {
        return secretUuid;
    }

    public void setSecretUuid(String secretUuid) {
        this.secretUuid = secretUuid;
    }
}

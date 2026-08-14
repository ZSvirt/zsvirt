package org.zstack.header.secret;

import org.zstack.header.message.MessageReply;

public class ResolveVtpmLibvirtSecretOnHypervisorReply extends MessageReply {
    private String secretUuid;

    public String getSecretUuid() {
        return secretUuid;
    }

    public void setSecretUuid(String secretUuid) {
        this.secretUuid = secretUuid;
    }
}

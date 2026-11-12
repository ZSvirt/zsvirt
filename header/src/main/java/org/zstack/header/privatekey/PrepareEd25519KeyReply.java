package org.zstack.header.privatekey;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.MessageReply;

public class PrepareEd25519KeyReply extends MessageReply {
    @NoLogging
    private String publicKey;

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}

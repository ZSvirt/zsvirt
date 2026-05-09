package org.zstack.header.secret;

import org.zstack.header.message.MessageReply;

public class SecretHostEnsureLuksSecretFileReply extends MessageReply {
    public static final String ERROR_CODE_KEYS_NOT_ON_DISK = "KEY_AGENT_KEYS_NOT_ON_DISK";
    public static final String ERROR_CODE_KEY_FILES_INTEGRITY_MISMATCH = "KEY_AGENT_KEY_FILES_INTEGRITY_MISMATCH";

    private String secFilePath;

    public String getSecFilePath() {
        return secFilePath;
    }

    public void setSecFilePath(String secFilePath) {
        this.secFilePath = secFilePath;
    }
}

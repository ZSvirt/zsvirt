package org.zstack.header.secret;

import org.zstack.header.message.MessageReply;

/** Reply for SecretHostDefineMsg (define secret on host for VM e.g. vTPM). */
public class SecretHostDefineReply extends MessageReply {
    public static final String ERROR_CODE_KEYS_NOT_ON_DISK = "KEY_AGENT_KEYS_NOT_ON_DISK";
    public static final String ERROR_CODE_KEY_FILES_INTEGRITY_MISMATCH = "KEY_AGENT_KEY_FILES_INTEGRITY_MISMATCH";

    private String secretUuid;

    public String getSecretUuid() {
        return secretUuid;
    }

    public void setSecretUuid(String secretUuid) {
        this.secretUuid = secretUuid;
    }
}

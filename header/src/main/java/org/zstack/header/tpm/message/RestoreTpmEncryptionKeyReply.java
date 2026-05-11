package org.zstack.header.tpm.message;

import org.zstack.header.message.MessageReply;

public class RestoreTpmEncryptionKeyReply extends MessageReply {
    private String tpmKeyBackupUuid;

    public String getTpmKeyBackupUuid() {
        return tpmKeyBackupUuid;
    }

    public void setTpmKeyBackupUuid(String tpmKeyBackupUuid) {
        this.tpmKeyBackupUuid = tpmKeyBackupUuid;
    }
}

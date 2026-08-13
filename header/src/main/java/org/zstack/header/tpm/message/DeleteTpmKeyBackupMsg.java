package org.zstack.header.tpm.message;

import org.zstack.header.message.NeedReplyMessage;

public class DeleteTpmKeyBackupMsg extends NeedReplyMessage {
    private String tpmUuid;
    private String tpmKeyBackupUuid;

    public String getTpmUuid() {
        return tpmUuid;
    }

    public void setTpmUuid(String tpmUuid) {
        this.tpmUuid = tpmUuid;
    }

    public String getTpmKeyBackupUuid() {
        return tpmKeyBackupUuid;
    }

    public void setTpmKeyBackupUuid(String tpmKeyBackupUuid) {
        this.tpmKeyBackupUuid = tpmKeyBackupUuid;
    }
}

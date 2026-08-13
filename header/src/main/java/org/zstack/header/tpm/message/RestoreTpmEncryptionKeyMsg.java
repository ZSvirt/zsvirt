package org.zstack.header.tpm.message;

import org.zstack.header.message.NeedReplyMessage;

public class RestoreTpmEncryptionKeyMsg extends NeedReplyMessage {
    private String srcResourceUuid;
    private String dstResourceUuid;
    /**
     * When true, the current encryption key on {@link #dstResourceUuid} (TPM) is copied to a
     * {@link org.zstack.header.tpm.entity.TpmKeyBackupVO} before restoring from {@link #srcResourceUuid}.
     */
    private boolean backupCurrentKey = true;

    public String getSrcResourceUuid() {
        return srcResourceUuid;
    }

    public void setSrcResourceUuid(String srcResourceUuid) {
        this.srcResourceUuid = srcResourceUuid;
    }

    public String getDstResourceUuid() {
        return dstResourceUuid;
    }

    public void setDstResourceUuid(String dstResourceUuid) {
        this.dstResourceUuid = dstResourceUuid;
    }

    public boolean isBackupCurrentKey() {
        return backupCurrentKey;
    }

    public void setBackupCurrentKey(boolean backupCurrentKey) {
        this.backupCurrentKey = backupCurrentKey;
    }
}

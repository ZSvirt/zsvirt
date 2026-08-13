package org.zstack.header.tpm.message;

import org.zstack.header.message.NeedReplyMessage;

public class BackupTpmEncryptionKeyMsg extends NeedReplyMessage {
    private String srcResourceUuid;
    private String dstResourceUuid;

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
}

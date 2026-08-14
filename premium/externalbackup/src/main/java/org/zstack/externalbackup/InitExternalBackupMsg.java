package org.zstack.externalbackup;

import org.zstack.header.message.NeedReplyMessage;

public class InitExternalBackupMsg extends NeedReplyMessage implements ExternalBackupMessage {
    private String uuid;

    @Override
    public String getExternalBackupUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
}

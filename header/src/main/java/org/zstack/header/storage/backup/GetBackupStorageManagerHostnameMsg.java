package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

public class GetBackupStorageManagerHostnameMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return uuid;
    }
}

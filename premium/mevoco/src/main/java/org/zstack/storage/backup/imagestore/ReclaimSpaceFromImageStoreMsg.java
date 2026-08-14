package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageMessage;

/**
 * Created by david on 2/25/17.
 */
public class ReclaimSpaceFromImageStoreMsg extends NeedReplyMessage implements BackupStorageMessage {
    private String uuid;

    @Override
    public String getBackupStorageUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

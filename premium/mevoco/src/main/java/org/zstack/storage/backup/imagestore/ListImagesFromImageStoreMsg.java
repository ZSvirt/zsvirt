package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageMessage;

/**
 * Created by mingjian.deng on 2017/8/31.
 */
public class ListImagesFromImageStoreMsg extends NeedReplyMessage implements BackupStorageMessage {
    String uuid;

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

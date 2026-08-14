package org.zstack.header.storage.backup;

import org.zstack.header.message.NeedReplyMessage;

public class RestoreImagesBackupStorageMetadataToDatabaseMsg extends NeedReplyMessage implements BackupStorageMessage {
    String imagesMetadata;
    String backupStorageUuid;

    public String getImagesMetadata() {
        return imagesMetadata;
    }

    public void setImagesMetadata(String imagesMetadata) {
        this.imagesMetadata = imagesMetadata;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}

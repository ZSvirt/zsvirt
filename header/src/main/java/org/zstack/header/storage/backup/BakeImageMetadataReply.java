package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

/**
 */
public class BakeImageMetadataReply extends MessageReply {
    private String imagesMetadata;
    private String backupStorageMetaFileName;

    public String getBackupStorageMetaFileName() {
        return backupStorageMetaFileName;
    }

    public void setBackupStorageMetaFileName(String backupStorageMetaFileName) {
        this.backupStorageMetaFileName = backupStorageMetaFileName;
    }

    public String getImagesMetadata() {
        return imagesMetadata;
    }

    public void setImagesMetadata(String imagesMetadata) {
        this.imagesMetadata = imagesMetadata;
    }
}

package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

public class CleanupAllVmMetadataOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String metadataDir;
    private long metadataGeneration;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getMetadataDir() {
        return metadataDir;
    }

    public void setMetadataDir(String metadataDir) {
        this.metadataDir = metadataDir;
    }

    public long getMetadataGeneration() {
        return metadataGeneration;
    }

    public void setMetadataGeneration(long metadataGeneration) {
        this.metadataGeneration = metadataGeneration;
    }
}

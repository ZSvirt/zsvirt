package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

public class CleanupAllVmMetadataOnPrimaryStorageReply extends MessageReply {
    private boolean skipped;
    private Long currentGeneration;

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public Long getCurrentGeneration() {
        return currentGeneration;
    }

    public void setCurrentGeneration(Long currentGeneration) {
        this.currentGeneration = currentGeneration;
    }
}

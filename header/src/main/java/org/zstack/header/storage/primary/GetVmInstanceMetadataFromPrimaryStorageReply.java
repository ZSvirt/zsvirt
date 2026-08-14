package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

public class GetVmInstanceMetadataFromPrimaryStorageReply extends MessageReply {
    private String metadata;

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}

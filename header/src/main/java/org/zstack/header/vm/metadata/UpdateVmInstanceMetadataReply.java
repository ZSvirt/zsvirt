package org.zstack.header.vm.metadata;

import org.zstack.header.message.MessageReply;

public class UpdateVmInstanceMetadataReply extends MessageReply {
    private String metadata;

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}

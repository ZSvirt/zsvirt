package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

public class ReadVmInstanceMetadataReply extends MessageReply {
    private String vmMetadata;

    public String getVmMetadata() {
        return vmMetadata;
    }

    public void setVmMetadata(String vmMetadata) {
        this.vmMetadata = vmMetadata;
    }
}

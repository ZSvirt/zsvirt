package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 */
public class AttachDataVolumeToVmReply extends MessageReply {
    private String hypervisorType;

    @Deprecated
    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
}

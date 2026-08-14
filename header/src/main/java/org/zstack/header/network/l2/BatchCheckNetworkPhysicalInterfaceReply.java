package org.zstack.header.network.l2;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class BatchCheckNetworkPhysicalInterfaceReply extends MessageReply {
    private List<String> physicalInterfaces;

    public List<String> getPhysicalInterfaces() {
        return physicalInterfaces;
    }

    public void setPhysicalInterfaces(List<String> physicalInterfaces) {
        this.physicalInterfaces = physicalInterfaces;
    }
}


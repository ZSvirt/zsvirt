package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.Map;

public class GetHostNumaTopologyReply extends MessageReply {
    private Map<String, HostNUMANode> numa;

    public void setNuma(Map<String, HostNUMANode> numa) {
        this.numa = numa;
    }

    public Map<String, HostNUMANode> getNuma() {
        return numa;
    }
}

package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.List;
import java.util.Map;

public class AllocateHostComputeResourceReply extends MessageReply {
    private String hostName;
    private List<Map<String, String>> pins;
    private String hostUuid;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public List<Map<String, String>> getPins() {
        return pins;
    }

    public void setPins(List<Map<String, String>> pins) {
        this.pins = pins;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}

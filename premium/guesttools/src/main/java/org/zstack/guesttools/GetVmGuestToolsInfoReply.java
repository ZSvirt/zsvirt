package org.zstack.guesttools;

import org.zstack.header.message.MessageReply;

import java.util.Map;

public class GetVmGuestToolsInfoReply extends MessageReply {
    private String version;
    private String status;
    private Map<String, String> features;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, String> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, String> features) {
        this.features = features;
    }
}

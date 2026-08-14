package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class UpdateHostnameMsg extends NeedReplyMessage implements HostMessage  {
    private String uuid;
    private String hostname;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }
}

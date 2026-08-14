package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

public class GetVmUptimeReply extends MessageReply {
    private String uptime;

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }
}


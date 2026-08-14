package org.zstack.header.host;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

public class ChangeHostPasswordMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;

    @NoLogging
    private String password;

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

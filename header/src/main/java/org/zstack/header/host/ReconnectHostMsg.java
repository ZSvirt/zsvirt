package org.zstack.header.host;

import org.zstack.header.message.ConfigurableTimeoutMessage;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.NeedReplyMessage;

import java.util.concurrent.TimeUnit;

@DefaultTimeout(timeunit = TimeUnit.MINUTES, value = 30)
public class ReconnectHostMsg extends NeedReplyMessage implements HostMessage, ConfigurableTimeoutMessage {
    private String hostUuid;
    private boolean skipIfHostConnected;

    public boolean isSkipIfHostConnected() {
        return skipIfHostConnected;
    }

    public void setSkipIfHostConnected(boolean skipIfHostConnected) {
        this.skipIfHostConnected = skipIfHostConnected;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}

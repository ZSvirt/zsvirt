package org.zstack.header.simulator;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class SimulatorHostConnectionControlMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private boolean disconnected;
    
    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
    }
}

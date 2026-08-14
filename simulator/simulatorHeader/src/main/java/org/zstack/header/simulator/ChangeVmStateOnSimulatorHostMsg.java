package org.zstack.header.simulator;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class ChangeVmStateOnSimulatorHostMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String vmInstanceUuid;
    private String vmState;
    
    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public String getVmUuid() {
        return vmInstanceUuid;
    }

    public void setVmUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVmState() {
        return vmState;
    }

    public void setVmState(String vmState) {
        this.vmState = vmState;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}

package org.zstack.header.vm;

import org.zstack.header.message.LocalEvent;

public class StrangerVmFoundEvent extends LocalEvent {
    private String vmIdentify;
    private String vmState;
    private String hostUuid;

    @Override
    public String getSubCategory() {
        return "StrangerVmFoundEvent";
    }

    public String getVmIdentify() {
        return vmIdentify;
    }

    public void setVmIdentify(String vmIdentify) {
        this.vmIdentify = vmIdentify;
    }

    public String getVmState() {
        return vmState;
    }

    public void setVmState(String vmState) {
        this.vmState = vmState;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}

package org.zstack.ha.message;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.NeedReplyMessage;

public class GetFencerStateFromHostMsg extends NeedReplyMessage {
    private HostInventory hostInventory;

    public HostInventory getHostInventory() {
        return hostInventory;
    }

    public void setHostInventory(HostInventory hostInventory) {
        this.hostInventory = hostInventory;
    }
}

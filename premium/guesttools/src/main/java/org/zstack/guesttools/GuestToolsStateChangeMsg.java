package org.zstack.guesttools;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;

public class GuestToolsStateChangeMsg extends NeedReplyMessage implements VmInstanceMessage {

    private GuestToolsStateInventory stateOld;
    private GuestToolsStateInventory stateNew;

    public GuestToolsStateInventory getQgaStateOld() {return stateOld;}

    public void setQgaStateOld(GuestToolsStateInventory stateOld) {this.stateOld = stateOld;}

    public GuestToolsStateInventory getQgaStateNew() {return stateNew;}

    public void setQgaStateNew(GuestToolsStateInventory stateNew) {this.stateNew = stateNew;}

    @Override
    public String getVmInstanceUuid() {
        return this.stateNew.getVmInstanceUuid();
    }
}

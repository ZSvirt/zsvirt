package org.zstack.header.host;

import org.zstack.header.message.LocalEvent;

public class HostStatusChangedEvent extends LocalEvent {
    private HostInventory inventory;
    private HostStatus previousState;
    private String reason;

    public HostStatusChangedEvent() {
    }

    public HostStatusChangedEvent(HostInventory inventory, HostStatus previousState) {
        super();
        this.inventory = inventory;
        this.previousState = previousState;
    }


    public HostInventory getInventory() {
        return inventory;
    }


    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }


    @Override
    public String getSubCategory() {
        return "HostConnectionStateChangedEvent";
    }

    public HostStatus getPreviousState() {
        return previousState;
    }

    public void setPreviousState(HostStatus previousState) {
        this.previousState = previousState;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

package org.zstack.pciDevice;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class CheckAndReservePciDeviceReply extends MessageReply {
    private List<String> reservedPciDevices;

    public List<String> getReservedPciDevices() {
        return reservedPciDevices;
    }

    public void setReservedPciDevices(List<String> reservedPciDevices) {
        this.reservedPciDevices = reservedPciDevices;
    }
}

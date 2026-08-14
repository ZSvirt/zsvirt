package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class CheckAndReserveMdevDeviceBySpecReply extends MessageReply {
    private List<String> reservedMdevDevices;

    public List<String> getReservedMdevDevices() {
        return reservedMdevDevices;
    }

    public void setReservedMdevDevices(List<String> reservedMdevDevices) {
        this.reservedMdevDevices = reservedMdevDevices;
    }
}

package org.zstack.compute.host;

import org.zstack.header.message.MessageReply;
import org.zstack.pciDevice.entity.PciDeviceInventory;

import java.util.List;

/**
 * Created by kayo on 2018/4/2.
 */
public class ReserveHostPciDeviceReply extends MessageReply {
    List<PciDeviceInventory> pciDevices;

    public List<PciDeviceInventory> getPciDevices() {
        return pciDevices;
    }

    public void setPciDevices(List<PciDeviceInventory> pciDevices) {
        this.pciDevices = pciDevices;
    }
}

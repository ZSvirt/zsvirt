package org.zstack.compute.host;

import org.zstack.header.message.MessageReply;
import org.zstack.pciDevice.PciDeviceInventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

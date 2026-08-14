package org.zstack.usbDevice;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by GuoYi on 10/21/17.
 */
@RestResponse(allTo = "inventories")
public class APIQueryUsbDeviceReply extends APIQueryReply {
    private List<UsbDeviceInventory> inventories;

    public static APIQueryUsbDeviceReply __example__() {
        APIQueryUsbDeviceReply reply = new APIQueryUsbDeviceReply();
        UsbDeviceInventory inv = new UsbDeviceInventory();
        inv.setUuid(uuid());
        inv.setName("usb");
        inv.setHostUuid(uuid());
        inv.setVmInstanceUuid(uuid());
        inv.setState(UsbDeviceState.Enabled);
        inv.setBusNum("001");
        inv.setDevNum("001");
        inv.setIdVendor("0781");
        inv.setIdProduct("5591");
        inv.setiManufacturer("SanDisk");
        inv.setiProduct("Ultra USB 3.0");
        inv.setiSerial("000000000001");
        inv.setUsbVersion("3.0");
        reply.setInventories(Arrays.asList(inv));
        return reply;
    }

    public List<UsbDeviceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<UsbDeviceInventory> inventories) {
        this.inventories = inventories;
    }
}

package org.zstack.usbDevice;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by GuoYi on 10/21/17.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetUsbDeviceCandidatesForAttachingVmReply extends APIReply {
    private List<UsbDeviceInventory> inventories;

    public static APIGetUsbDeviceCandidatesForAttachingVmReply __example__() {
        APIGetUsbDeviceCandidatesForAttachingVmReply reply = new APIGetUsbDeviceCandidatesForAttachingVmReply();
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

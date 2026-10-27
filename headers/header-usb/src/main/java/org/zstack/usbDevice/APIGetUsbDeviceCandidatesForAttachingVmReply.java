package org.zstack.usbDevice;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.usbDevice.entity.UsbDeviceInventory;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by GuoYi on 10/21/17.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetUsbDeviceCandidatesForAttachingVmReply extends APIReply {
    private List<UsbDeviceInventory> inventories;

    public static APIGetUsbDeviceCandidatesForAttachingVmReply __example__() {
        APIGetUsbDeviceCandidatesForAttachingVmReply reply = new APIGetUsbDeviceCandidatesForAttachingVmReply();
        reply.setInventories(list(UsbDeviceInventory.__example__()));
        return reply;
    }

    public List<UsbDeviceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<UsbDeviceInventory> inventories) {
        this.inventories = inventories;
    }
}

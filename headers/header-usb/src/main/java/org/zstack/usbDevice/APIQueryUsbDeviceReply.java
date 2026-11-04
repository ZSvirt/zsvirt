package org.zstack.usbDevice;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.usbDevice.entity.UsbDeviceInventory;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by GuoYi on 10/21/17.
 */
@RestResponse(allTo = "inventories")
public class APIQueryUsbDeviceReply extends APIQueryReply {
    private List<UsbDeviceInventory> inventories;

    public static APIQueryUsbDeviceReply __example__() {
        APIQueryUsbDeviceReply reply = new APIQueryUsbDeviceReply();
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

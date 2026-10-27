package org.zstack.usbDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.usbDevice.entity.UsbDeviceInventory;

/**
 * Created by GuoYi on 10/21/17.
 */
@RestResponse(allTo = "inventory")
public class APIDetachUsbDeviceFromVmEvent extends APIEvent {
    private UsbDeviceInventory inventory;

    public APIDetachUsbDeviceFromVmEvent() {
    }

    public APIDetachUsbDeviceFromVmEvent(String apiId) {
        super(apiId);
    }

    public static APIDetachUsbDeviceFromVmEvent __example__() {
        APIDetachUsbDeviceFromVmEvent evt = new APIDetachUsbDeviceFromVmEvent();
        evt.setInventory(UsbDeviceInventory.__example__());
        return evt;
    }

    public UsbDeviceInventory getInventory() {
        return inventory;
    }

    public void setInventory(UsbDeviceInventory inventory) {
        this.inventory = inventory;
    }
}

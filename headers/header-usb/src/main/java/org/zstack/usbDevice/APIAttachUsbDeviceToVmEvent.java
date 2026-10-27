package org.zstack.usbDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.usbDevice.entity.UsbDeviceInventory;

/**
 * Created by GuoYi on 10/21/17.
 */
@RestResponse(allTo = "inventory")
public class APIAttachUsbDeviceToVmEvent extends APIEvent {
    private UsbDeviceInventory inventory;

    public APIAttachUsbDeviceToVmEvent() {
    }

    public APIAttachUsbDeviceToVmEvent(String apiId) {
        super(apiId);
    }

    public static APIAttachUsbDeviceToVmEvent __example__() {
        APIAttachUsbDeviceToVmEvent evt = new APIAttachUsbDeviceToVmEvent();
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

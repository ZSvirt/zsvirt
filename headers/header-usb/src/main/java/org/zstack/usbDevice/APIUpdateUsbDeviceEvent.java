package org.zstack.usbDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.usbDevice.entity.UsbDeviceInventory;

/**
 * Created by GuoYi on 10/24/2017.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateUsbDeviceEvent extends APIEvent {
    private UsbDeviceInventory inventory;

    public APIUpdateUsbDeviceEvent() {
    }

    public APIUpdateUsbDeviceEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateUsbDeviceEvent __example__() {
        APIUpdateUsbDeviceEvent event = new APIUpdateUsbDeviceEvent();
        event.setInventory(UsbDeviceInventory.__example__());
        return event;
    }

    public UsbDeviceInventory getInventory() {
        return inventory;
    }

    public void setInventory(UsbDeviceInventory inventory) {
        this.inventory = inventory;
    }
}

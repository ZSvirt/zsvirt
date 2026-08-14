package org.zstack.usbDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

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

        UsbDeviceInventory inv = new UsbDeviceInventory();
        inv.setUuid(uuid());
        inv.setHostUuid(uuid());
        inv.setVmInstanceUuid(uuid());
        inv.setIdVendor("10de");
        inv.setIdProduct("0e0f");
        inv.setiManufacturer("SanDisk");
        inv.setiProduct("USB Storage");
        inv.setiSerial("000000000001");
        inv.setUsbVersion("3.0");
        inv.setState(UsbDeviceState.Disabled);
        inv.setDescription("test usb");

        event.setInventory(inv);
        return event;
    }

    public UsbDeviceInventory getInventory() {
        return inventory;
    }

    public void setInventory(UsbDeviceInventory inventory) {
        this.inventory = inventory;
    }
}

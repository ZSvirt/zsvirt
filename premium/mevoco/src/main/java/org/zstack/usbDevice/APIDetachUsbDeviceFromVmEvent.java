package org.zstack.usbDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

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
        evt.setInventory(inv);
        return evt;
    }

    public UsbDeviceInventory getInventory() {
        return inventory;
    }

    public void setInventory(UsbDeviceInventory inventory) {
        this.inventory = inventory;
    }
}

package org.zstack.pciDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 10/07/2017.
 */
@RestResponse(allTo = "inventory")
public class APICreatePciDeviceOfferingEvent extends APIEvent {
    private PciDeviceOfferingInventory inventory;

    public APICreatePciDeviceOfferingEvent() {
    }

    public APICreatePciDeviceOfferingEvent(String apiId) {
        super(apiId);
    }

    public PciDeviceOfferingInventory getInventory() {
        return inventory;
    }

    public void setInventory(PciDeviceOfferingInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreatePciDeviceOfferingEvent __example__() {
        APICreatePciDeviceOfferingEvent event = new APICreatePciDeviceOfferingEvent();
        PciDeviceOfferingInventory inv = new PciDeviceOfferingInventory();
        inv.setType(PciDeviceOfferingType.GPU_Video);
        inv.setVendorId("10de");
        inv.setDeviceId("0e0f");
        inv.setSubdeviceId("118b");
        inv.setSubvendorId("10de");
        event.setInventory(inv);
        return event;
    }
}

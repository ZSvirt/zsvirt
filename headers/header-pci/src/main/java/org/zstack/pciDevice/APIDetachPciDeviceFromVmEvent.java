package org.zstack.pciDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.pciDevice.entity.PciDeviceInventory;

/**
 * Created by weiwang on 10/07/2017.
 */
@RestResponse(allTo = "inventory")
public class APIDetachPciDeviceFromVmEvent extends APIEvent {
    private PciDeviceInventory inventory;

    public APIDetachPciDeviceFromVmEvent() {
    }

    public APIDetachPciDeviceFromVmEvent(String apiId) {
        super(apiId);
    }

    public PciDeviceInventory getInventory() {
        return inventory;
    }

    public void setInventory(PciDeviceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIDetachPciDeviceFromVmEvent __example__() {
        APIDetachPciDeviceFromVmEvent event = new APIDetachPciDeviceFromVmEvent();
        event.setInventory(PciDeviceInventory.__example__());
        return event;
    }
}

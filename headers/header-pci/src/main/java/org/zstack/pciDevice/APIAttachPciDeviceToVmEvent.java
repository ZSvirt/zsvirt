package org.zstack.pciDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.pciDevice.entity.PciDeviceInventory;

/**
 * Created by weiwang on 10/07/2017.
 */
@RestResponse(allTo = "inventory")
public class APIAttachPciDeviceToVmEvent extends APIEvent {
    private PciDeviceInventory inventory;

    public APIAttachPciDeviceToVmEvent() {
    }

    public APIAttachPciDeviceToVmEvent(String apiId) {
        super(apiId);
    }

    public PciDeviceInventory getInventory() {
        return inventory;
    }

    public void setInventory(PciDeviceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAttachPciDeviceToVmEvent __example__() {
        APIAttachPciDeviceToVmEvent event = new APIAttachPciDeviceToVmEvent();
        event.setInventory(PciDeviceInventory.__example__());
        return event;
    }
}

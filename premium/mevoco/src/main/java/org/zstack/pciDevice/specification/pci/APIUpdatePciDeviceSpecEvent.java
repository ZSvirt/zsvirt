package org.zstack.pciDevice.specification.pci;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-03-06.
 */
@RestResponse(allTo = "inventory")
public class APIUpdatePciDeviceSpecEvent extends APIEvent {
    private PciDeviceSpecInventory inventory;

    public APIUpdatePciDeviceSpecEvent() {
    }

    public APIUpdatePciDeviceSpecEvent(String apiId) {
        super(apiId);
    }

    public PciDeviceSpecInventory getInventory() {
        return inventory;
    }

    public void setInventory(PciDeviceSpecInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdatePciDeviceSpecEvent __example__() {
        APIUpdatePciDeviceSpecEvent evt = new APIUpdatePciDeviceSpecEvent();
        evt.setInventory(PciDeviceSpecInventory.__example__());
        return evt;
    }
}

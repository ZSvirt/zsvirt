package org.zstack.pciDevice.specification.pci;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-05-22.
 */
@RestResponse(allTo = "inventory")
public class APIAddPciDeviceSpecToVmInstanceEvent extends APIEvent {
    private VmInstancePciDeviceSpecRefInventory inventory;

    public APIAddPciDeviceSpecToVmInstanceEvent() {
    }

    public APIAddPciDeviceSpecToVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public VmInstancePciDeviceSpecRefInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstancePciDeviceSpecRefInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddPciDeviceSpecToVmInstanceEvent __example__() {
        APIAddPciDeviceSpecToVmInstanceEvent event = new APIAddPciDeviceSpecToVmInstanceEvent();
        event.setInventory(VmInstancePciDeviceSpecRefInventory.__example__());
        return event;
    }
}

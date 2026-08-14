package org.zstack.pciDevice.specification.mdev;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-05-22.
 */
@RestResponse(allTo = "inventory")
public class APIAddMdevDeviceSpecToVmInstanceEvent extends APIEvent {
    private VmInstanceMdevDeviceSpecRefInventory inventory;

    public APIAddMdevDeviceSpecToVmInstanceEvent() {
    }

    public APIAddMdevDeviceSpecToVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public VmInstanceMdevDeviceSpecRefInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceMdevDeviceSpecRefInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddMdevDeviceSpecToVmInstanceEvent __example__() {
        APIAddMdevDeviceSpecToVmInstanceEvent event = new APIAddMdevDeviceSpecToVmInstanceEvent();
        event.setInventory(VmInstanceMdevDeviceSpecRefInventory.__example__());
        return event;
    }
}

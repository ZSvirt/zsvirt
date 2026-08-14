package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-04-19.
 */
@RestResponse(allTo = "inventory")
public class APIDetachMdevDeviceFromVmEvent extends APIEvent {
    private MdevDeviceInventory inventory;

    public APIDetachMdevDeviceFromVmEvent() {
    }

    public APIDetachMdevDeviceFromVmEvent(String apiId) {
        super(apiId);
    }

    public MdevDeviceInventory getInventory() {
        return inventory;
    }

    public void setInventory(MdevDeviceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIDetachMdevDeviceFromVmEvent __example__() {
        APIDetachMdevDeviceFromVmEvent evt = new APIDetachMdevDeviceFromVmEvent();
        evt.setInventory(MdevDeviceInventory.__example__());
        return evt;
    }
}

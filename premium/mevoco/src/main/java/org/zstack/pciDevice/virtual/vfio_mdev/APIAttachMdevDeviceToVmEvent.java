package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-04-20.
 */
@RestResponse(allTo = "inventory")
public class APIAttachMdevDeviceToVmEvent extends APIEvent {
    private MdevDeviceInventory inventory;

    public APIAttachMdevDeviceToVmEvent() {
    }

    public APIAttachMdevDeviceToVmEvent(String apiId) {
        super(apiId);
    }

    public MdevDeviceInventory getInventory() {
        return inventory;
    }

    public void setInventory(MdevDeviceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAttachMdevDeviceToVmEvent __example__() {
        APIAttachMdevDeviceToVmEvent evt = new APIAttachMdevDeviceToVmEvent();
        evt.setInventory(MdevDeviceInventory.__example__());
        return evt;
    }
}

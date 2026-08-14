package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-04-28.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateMdevDeviceEvent extends APIEvent {
    private MdevDeviceInventory inventory;

    public APIUpdateMdevDeviceEvent() {
    }

    public APIUpdateMdevDeviceEvent(String apiId) {
        super(apiId);
    }

    public MdevDeviceInventory getInventory() {
        return inventory;
    }

    public void setInventory(MdevDeviceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateMdevDeviceEvent __example__() {
        APIUpdateMdevDeviceEvent evt = new APIUpdateMdevDeviceEvent();
        evt.setInventory(MdevDeviceInventory.__example__());
        return evt;
    }
}

package org.zstack.pciDevice.specification.mdev;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-05-05.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateMdevDeviceSpecEvent extends APIEvent {
    private MdevDeviceSpecInventory inventory;

    public APIUpdateMdevDeviceSpecEvent() {
    }

    public APIUpdateMdevDeviceSpecEvent(String apiId) {
        super(apiId);
    }

    public MdevDeviceSpecInventory getInventory() {
        return inventory;
    }

    public void setInventory(MdevDeviceSpecInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateMdevDeviceSpecEvent __example__() {
        APIUpdateMdevDeviceSpecEvent evt = new APIUpdateMdevDeviceSpecEvent();
        evt.setInventory(MdevDeviceSpecInventory.__example__());
        return evt;
    }
}

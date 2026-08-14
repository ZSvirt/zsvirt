
package org.zstack.header.volume.block;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateXskyBlockVolumeEvent extends APIEvent {
    private XskyBlockVolumeInventory inventory;

    public void setInventory(XskyBlockVolumeInventory inventory) {
        this.inventory = inventory;
    }

    public XskyBlockVolumeInventory getInventory() {
        return inventory;
    }

    public APIUpdateXskyBlockVolumeEvent() {
    }

    public APIUpdateXskyBlockVolumeEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateXskyBlockVolumeEvent __example__() {
        APIUpdateXskyBlockVolumeEvent event = new APIUpdateXskyBlockVolumeEvent();
        return event;
    }
}

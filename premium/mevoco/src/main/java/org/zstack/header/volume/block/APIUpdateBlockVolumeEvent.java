
package org.zstack.header.volume.block;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateBlockVolumeEvent extends APIEvent {
    private BlockVolumeInventory inventory;

    public void setInventory(BlockVolumeInventory inventory) {
        this.inventory = inventory;
    }

    public BlockVolumeInventory getInventory() {
        return inventory;
    }

    public APIUpdateBlockVolumeEvent() {
    }

    public APIUpdateBlockVolumeEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateBlockVolumeEvent __example__() {
        APIUpdateBlockVolumeEvent event = new APIUpdateBlockVolumeEvent();
        return event;
    }
}

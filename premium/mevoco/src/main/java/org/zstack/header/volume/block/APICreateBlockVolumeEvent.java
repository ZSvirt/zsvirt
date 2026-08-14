
package org.zstack.header.volume.block;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateBlockVolumeEvent extends APIEvent {
    private BlockVolumeInventory inventory;

    public APICreateBlockVolumeEvent() {
    }

    public APICreateBlockVolumeEvent(String apiId) {
        super(apiId);
    }

    public BlockVolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(BlockVolumeInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateBlockVolumeEvent __example__() {
        APICreateBlockVolumeEvent event = new APICreateBlockVolumeEvent();
        BlockVolumeInventory blockVolumeInventory = new BlockVolumeInventory();
        event.setInventory(blockVolumeInventory);
        return event;
    }
}

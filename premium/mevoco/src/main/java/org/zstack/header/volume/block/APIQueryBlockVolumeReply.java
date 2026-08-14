
package org.zstack.header.volume.block;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryBlockVolumeReply extends APIQueryReply {

    private List<BlockVolumeInventory> inventories;

    public List<BlockVolumeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<BlockVolumeInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryBlockVolumeReply __example__() {
        APIQueryBlockVolumeReply reply = new APIQueryBlockVolumeReply();
        return reply;
    }
}

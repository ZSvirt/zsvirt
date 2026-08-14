package org.zstack.header.volume.block;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryExponBlockVolumeReply extends APIQueryReply {

    private List<ExponBlockVolumeInventory> inventories;

    public List<ExponBlockVolumeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ExponBlockVolumeInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryExponBlockVolumeReply __example__() {
        APIQueryExponBlockVolumeReply reply = new APIQueryExponBlockVolumeReply();
        return reply;
    }
}


package org.zstack.header.volume.block;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * @author shenjin
 * @date 2023/6/24 15:36
 */
@RestResponse(allTo = "inventories")
public class APIQueryXskyBlockVolumeReply extends APIQueryReply {

    private List<XskyBlockVolumeInventory> inventories;

    public List<XskyBlockVolumeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<XskyBlockVolumeInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryXskyBlockVolumeReply __example__() {
        APIQueryXskyBlockVolumeReply reply = new APIQueryXskyBlockVolumeReply();
        return reply;
    }
}


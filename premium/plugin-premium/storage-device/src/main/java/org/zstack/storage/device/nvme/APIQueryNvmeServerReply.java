package org.zstack.storage.device.nvme;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryNvmeServerReply extends APIQueryReply {
    private List<NvmeServerInventory> inventories;

    public List<NvmeServerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NvmeServerInventory> inventories) {
        this.inventories = inventories;
    }
}

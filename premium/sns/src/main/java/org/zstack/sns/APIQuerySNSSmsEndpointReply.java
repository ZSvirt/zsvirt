package org.zstack.sns;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by Qi Le on 2019-07-23
 */
@RestResponse(allTo = "inventories")
public class APIQuerySNSSmsEndpointReply extends APIQueryReply {
    private List<SNSSmsEndpointInventory> inventories;

    public List<SNSSmsEndpointInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSSmsEndpointInventory> inventories) {
        this.inventories = inventories;
    }
}

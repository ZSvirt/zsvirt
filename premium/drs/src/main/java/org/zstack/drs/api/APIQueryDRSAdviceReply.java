package org.zstack.drs.api;

import org.zstack.drs.entity.DRSAdviceInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by lining on 2019/12/12.
 */
@RestResponse(allTo = "inventories")
public class APIQueryDRSAdviceReply extends APIQueryReply {
    private List<DRSAdviceInventory> inventories;

    public List<DRSAdviceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<DRSAdviceInventory> inventories) {
        this.inventories = inventories;
    }
}

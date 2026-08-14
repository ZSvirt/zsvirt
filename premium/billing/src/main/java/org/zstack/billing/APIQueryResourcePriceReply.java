package org.zstack.billing;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/5/14.
 */
@RestResponse(allTo = "inventories")
public class APIQueryResourcePriceReply extends APIQueryReply {
    private List<PriceInventory> inventories;

    public List<PriceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PriceInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryResourcePriceReply __example__() {
        APIQueryResourcePriceReply reply = new APIQueryResourcePriceReply();
        PriceInventory inventory = new PriceInventory();
        inventory.setUuid(uuid());
        inventory.setPrice(2d);
        inventory.setTimeUnit("s");
        inventory.setResourceName("Volume");
        inventory.setResourceUnit("1");

        reply.setInventories(list(inventory));
        return reply;
    }

}

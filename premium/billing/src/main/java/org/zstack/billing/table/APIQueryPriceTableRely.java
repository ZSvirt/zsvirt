package org.zstack.billing.table;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import java.sql.Timestamp;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by lining on 2019/9/10.
 */
@RestResponse(allTo = "inventories")
public class APIQueryPriceTableRely extends APIQueryReply {
    private List<PriceTableInventory> inventories;

    public List<PriceTableInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PriceTableInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryPriceTableRely __example__() {
        APIQueryPriceTableRely reply = new APIQueryPriceTableRely();
        PriceTableInventory inventory = new PriceTableInventory();
        inventory.setUuid(uuid());
        inventory.setName("price table");
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(list(inventory));
        return reply;
    }

}

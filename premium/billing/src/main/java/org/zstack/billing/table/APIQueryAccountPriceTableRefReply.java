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
public class APIQueryAccountPriceTableRefReply extends APIQueryReply {
    private List<AccountPriceTableRefInventory> inventories;

    public List<AccountPriceTableRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AccountPriceTableRefInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryAccountPriceTableRefReply __example__() {
        APIQueryAccountPriceTableRefReply reply = new APIQueryAccountPriceTableRefReply();
        AccountPriceTableRefInventory inventory = new AccountPriceTableRefInventory();
        inventory.setAccountUuid(uuid());
        inventory.setTableUuid(uuid());
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(list(inventory));
        return reply;
    }

}

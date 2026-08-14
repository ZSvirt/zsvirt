package org.zstack.billing;

import org.zstack.billing.generator.BillingInventory;
import org.zstack.billing.generator.BillingType;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by lining on 2019/5/27.
 */
@RestResponse(allTo = "inventories")
public class APIQueryAccountBillingReply extends APIQueryReply {
    private List<BillingInventory> inventories;

    public List<BillingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<BillingInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryAccountBillingReply __example__() {
        APIQueryAccountBillingReply reply = new APIQueryAccountBillingReply();
        BillingInventory inventory = new BillingInventory();
        inventory.setAccountUuid(uuid());
        inventory.setBillingType(BillingType.DataVolume.toString());
        inventory.setEndTime(1559017375886L);
        inventory.setStartTime(1559017175886L);
        inventory.setId(1);
        inventory.setSpending(100);
        inventory.setResourceUuid(uuid());
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setHypervisorType("KVM");

        reply.setInventories(list(inventory));
        return reply;
    }

}

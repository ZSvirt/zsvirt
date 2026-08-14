package org.zstack.header.configuration;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.primary.PrimaryStorageConstant;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryDiskOfferingReply extends APIQueryReply {
    private List<DiskOfferingInventory> inventories;

    public List<DiskOfferingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<DiskOfferingInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryDiskOfferingReply __example__() {
        APIQueryDiskOfferingReply reply = new APIQueryDiskOfferingReply();
        DiskOfferingInventory inventory = new DiskOfferingInventory();
        inventory.setName("diskOffering1");
        inventory.setDiskSize(100);
        inventory.setUuid(uuid());
        inventory.setAllocatorStrategy(PrimaryStorageConstant.DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE);
        inventory.setType("DefaultDiskOfferingType");
        inventory.setState(DiskOfferingState.Enabled.toString());

        reply.setInventories(list(inventory));
        return reply;
    }

}

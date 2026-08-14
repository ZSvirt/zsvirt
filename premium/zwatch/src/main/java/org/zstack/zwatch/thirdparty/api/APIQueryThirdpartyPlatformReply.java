package org.zstack.zwatch.thirdparty.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformInventory;

import java.sql.Timestamp;
import java.util.List;
import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryThirdpartyPlatformReply extends APIQueryReply {
    private List<ThirdpartyPlatformInventory> inventories;

    public static APIQueryThirdpartyPlatformReply __example__() {
        APIQueryThirdpartyPlatformReply ret = new APIQueryThirdpartyPlatformReply();
        ThirdpartyPlatformInventory inventory = ThirdpartyPlatformInventory.__example__();
        inventory.setUuid(uuid());
        inventory.setLastSyncDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<ThirdpartyPlatformInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ThirdpartyPlatformInventory> inventories) {
        this.inventories = inventories;
    }
}

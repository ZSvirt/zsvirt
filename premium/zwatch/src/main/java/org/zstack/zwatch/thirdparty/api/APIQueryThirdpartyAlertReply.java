package org.zstack.zwatch.thirdparty.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyOriginalAlertInventory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryThirdpartyAlertReply extends APIQueryReply {
    private List<ThirdpartyOriginalAlertInventory> inventories;

    public static APIQueryThirdpartyAlertReply __example__() {
        APIQueryThirdpartyAlertReply ret = new APIQueryThirdpartyAlertReply();
        ThirdpartyOriginalAlertInventory inventory = ThirdpartyOriginalAlertInventory.__example__();
        inventory.setUuid(uuid());
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<ThirdpartyOriginalAlertInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ThirdpartyOriginalAlertInventory> inventories) {
        this.inventories = inventories;
    }
}

package org.zstack.core.config;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import java.util.List;
import static java.util.Arrays.asList;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryGlobalConfigReply extends APIQueryReply {
    private List<GlobalConfigInventory> inventories;

    public List<GlobalConfigInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<GlobalConfigInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryGlobalConfigReply __example__() {
        APIQueryGlobalConfigReply reply = new APIQueryGlobalConfigReply();
        GlobalConfigInventory inv = new GlobalConfigInventory();
        inv.setCategory("backupStorage");
        inv.setDefaultValue("1G");
        inv.setName("reservedCapacity");
        inv.setValue("2G");
        inv.setDescription("Reserved capcacity on every backup storage");
        reply.setInventories(asList(inv));
        return reply;
    }

}

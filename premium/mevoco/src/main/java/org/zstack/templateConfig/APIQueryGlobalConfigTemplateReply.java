package org.zstack.templateConfig;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import java.util.List;
import static java.util.Arrays.asList;


@RestResponse(allTo = "inventories")
public class APIQueryGlobalConfigTemplateReply extends APIQueryReply {
    private List<GlobalConfigTemplateInventory> inventories;

    public List<GlobalConfigTemplateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<GlobalConfigTemplateInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryGlobalConfigTemplateReply __example__() {
        APIQueryGlobalConfigTemplateReply reply = new APIQueryGlobalConfigTemplateReply();
        GlobalConfigTemplateInventory inv = new GlobalConfigTemplateInventory();
        inv.setUuid("reservedCapacity");
        inv.setName("scenes1");
        inv.setType("System");
        inv.setDescription("For scenes1");
        reply.setInventories(asList(inv));
        return reply;
    }
}

package org.zstack.templateConfig;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import java.util.List;
import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryTemplateConfigReply extends APIQueryReply {
    private List<TemplateConfigInventory> inventories;

    public List<TemplateConfigInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<TemplateConfigInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryTemplateConfigReply __example__() {
        APIQueryTemplateConfigReply reply = new APIQueryTemplateConfigReply();
        TemplateConfigInventory inv = new TemplateConfigInventory();
        inv.setTemplateUuid("0000111122223333");
        inv.setCategory("backupStorage");
        inv.setName("reservedCapacity");
        inv.setDefaultValue("1G");
        inv.setValue("2G");
        reply.setInventories(asList(inv));
        return reply;
    }
}

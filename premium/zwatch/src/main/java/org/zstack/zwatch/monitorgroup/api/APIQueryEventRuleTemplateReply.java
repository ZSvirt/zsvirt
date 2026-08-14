package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.EventRuleTemplateInventory;

import java.util.List;
import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryEventRuleTemplateReply extends APIQueryReply {
    private List<EventRuleTemplateInventory> inventories;

    public static APIQueryEventRuleTemplateReply __example__() {
        APIQueryEventRuleTemplateReply ret = new APIQueryEventRuleTemplateReply();
        EventRuleTemplateInventory inventory = APIAddEventRuleTemplateEvent.__example__().getInventory();
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<EventRuleTemplateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<EventRuleTemplateInventory> inventories) {
        this.inventories = inventories;
    }
}

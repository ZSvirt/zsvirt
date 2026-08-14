package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateInventory;

import java.util.List;
import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryMonitorTemplateReply extends APIQueryReply {
    private List<MonitorTemplateInventory> inventories;

    public static APIQueryMonitorTemplateReply __example__() {
        APIQueryMonitorTemplateReply ret = new APIQueryMonitorTemplateReply();
        MonitorTemplateInventory inventory = APICreateMonitorTemplateEvent.__example__().getInventory();
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<MonitorTemplateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MonitorTemplateInventory> inventories) {
        this.inventories = inventories;
    }
}

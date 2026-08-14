package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupInventory;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryMonitorGroupReply extends APIQueryReply {
    private List<MonitorGroupInventory> inventories;

    public static APIQueryMonitorGroupReply __example__() {
        APIQueryMonitorGroupReply ret = new APIQueryMonitorGroupReply();
        MonitorGroupInventory inventory = APICreateMonitorGroupEvent.__example__().getInventory();
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<MonitorGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MonitorGroupInventory> inventories) {
        this.inventories = inventories;
    }
}

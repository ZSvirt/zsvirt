package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupInstanceInventory;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryMonitorGroupInstanceReply extends APIQueryReply {
    private List<MonitorGroupInstanceInventory> inventories;

    public static APIQueryMonitorGroupInstanceReply __example__() {
        APIQueryMonitorGroupInstanceReply ret = new APIQueryMonitorGroupInstanceReply();
        MonitorGroupInstanceInventory inventory = APIAddInstanceToMonitorGroupEvent.__example__().getInventory();
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<MonitorGroupInstanceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MonitorGroupInstanceInventory> inventories) {
        this.inventories = inventories;
    }
}

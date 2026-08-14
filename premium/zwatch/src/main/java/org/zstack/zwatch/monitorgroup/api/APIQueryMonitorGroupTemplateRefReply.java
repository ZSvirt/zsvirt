package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupInventory;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupTemplateRefInventory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryMonitorGroupTemplateRefReply extends APIQueryReply {
    private List<MonitorGroupTemplateRefInventory> inventories;

    public static APIQueryMonitorGroupTemplateRefReply __example__() {
        APIQueryMonitorGroupTemplateRefReply ret = new APIQueryMonitorGroupTemplateRefReply();
        MonitorGroupTemplateRefInventory inventory = APIApplyMonitorTemplateToMonitorGroupEvent.__example__().getInventory();
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<MonitorGroupTemplateRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MonitorGroupTemplateRefInventory> inventories) {
        this.inventories = inventories;
    }
}

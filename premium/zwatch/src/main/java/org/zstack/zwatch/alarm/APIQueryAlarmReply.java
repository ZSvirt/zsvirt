package org.zstack.zwatch.alarm;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryAlarmReply extends APIQueryReply {
    private List<AlarmInventory> inventories;

    public static APIQueryAlarmReply __example__() {
        APIQueryAlarmReply ret = new APIQueryAlarmReply();
        ret.inventories = asList(AlarmInventory.__example__());
        return ret;
    }

    public List<AlarmInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AlarmInventory> inventories) {
        this.inventories = inventories;
    }
}

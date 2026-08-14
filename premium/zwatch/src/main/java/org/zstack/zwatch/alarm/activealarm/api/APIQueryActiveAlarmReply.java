package org.zstack.zwatch.alarm.activealarm.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmInventory;
import java.sql.Timestamp;
import java.util.List;
import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryActiveAlarmReply extends APIQueryReply {
    private List<ActiveAlarmInventory> inventories;

    public static APIQueryActiveAlarmReply __example__() {
        APIQueryActiveAlarmReply ret = new APIQueryActiveAlarmReply();
        ActiveAlarmInventory inventory = new ActiveAlarmInventory();
        inventory.setUuid(uuid());
        inventory.setAlarmUuid("d4904ace98f834e7bf3485376742133f");
        inventory.setTemplateUuid(uuid());
        inventory.setNamespace("ZStack/VM");
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ret.inventories = asList(inventory);
        return ret;
    }

    public List<ActiveAlarmInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ActiveAlarmInventory> inventories) {
        this.inventories = inventories;
    }
}

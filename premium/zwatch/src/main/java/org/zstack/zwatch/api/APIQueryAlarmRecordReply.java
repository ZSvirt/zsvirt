package org.zstack.zwatch.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.migratedb.AlarmRecordsInventory;

import java.util.List;


@RestResponse(allTo = "inventories")
public class APIQueryAlarmRecordReply extends APIQueryReply {
    private List<AlarmRecordsInventory> inventories;

    public List<AlarmRecordsInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AlarmRecordsInventory> inventories) {
        this.inventories = inventories;
    }
}

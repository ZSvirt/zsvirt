package org.zstack.storage.device.localRaid;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryPhysicalDriveSelfTestHistoryReply extends APIQueryReply {
    private List<PhysicalDriveSmartSelfTestHistoryInventory> inventories;

    public List<PhysicalDriveSmartSelfTestHistoryInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PhysicalDriveSmartSelfTestHistoryInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryPhysicalDriveSelfTestHistoryReply __example__() {
        APIQueryPhysicalDriveSelfTestHistoryReply reply = new APIQueryPhysicalDriveSelfTestHistoryReply();

        PhysicalDriveSmartSelfTestHistoryInventory inventory1 = new PhysicalDriveSmartSelfTestHistoryInventory();
        inventory1.setId(1L);
        inventory1.setRaidPhysicalDriveUuid(uuid());
        inventory1.setRunningState(RunningState.Success);
        inventory1.setTestResult("Completed without error");
        inventory1.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory1.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        PhysicalDriveSmartSelfTestHistoryInventory inventory2 = new PhysicalDriveSmartSelfTestHistoryInventory();
        inventory2.setId(2L);
        inventory2.setRaidPhysicalDriveUuid(uuid());
        inventory2.setRunningState(RunningState.Success);
        inventory2.setTestResult("Completed without error");
        inventory2.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory2.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(asList(inventory1, inventory2));
        reply.setSuccess(true);
        return reply;
    }
}

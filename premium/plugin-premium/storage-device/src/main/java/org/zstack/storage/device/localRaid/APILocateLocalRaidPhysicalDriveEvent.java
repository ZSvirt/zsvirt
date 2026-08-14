package org.zstack.storage.device.localRaid;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Create by weiwang at 2018/10/18
 */

@RestResponse(fieldsTo = {"inventory"})
public class APILocateLocalRaidPhysicalDriveEvent extends APIEvent {
    private RaidPhysicalDriveInventory inventory;

    public RaidPhysicalDriveInventory getInventory() {
        return inventory;
    }

    public void setInventory(RaidPhysicalDriveInventory inventory) {
        this.inventory = inventory;
    }

    public APILocateLocalRaidPhysicalDriveEvent() {
    }

    public APILocateLocalRaidPhysicalDriveEvent(String apiId) {
        super(apiId);
    }

    public static APILocateLocalRaidPhysicalDriveEvent __example__() {
        APILocateLocalRaidPhysicalDriveEvent evt = new APILocateLocalRaidPhysicalDriveEvent();
        return evt;
    }
}

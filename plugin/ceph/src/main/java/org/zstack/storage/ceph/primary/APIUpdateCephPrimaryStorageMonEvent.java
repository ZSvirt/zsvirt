package org.zstack.storage.ceph.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;

import java.sql.Timestamp;
import java.util.Collections;

/**
 * Created by frank on 8/6/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateCephPrimaryStorageMonEvent extends APIEvent {
    private CephPrimaryStorageInventory inventory;

    public APIUpdateCephPrimaryStorageMonEvent() {
    }

    public APIUpdateCephPrimaryStorageMonEvent(String apiId) {
        super(apiId);
    }

    public CephPrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(CephPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateCephPrimaryStorageMonEvent __example__() {
        APIUpdateCephPrimaryStorageMonEvent event = new APIUpdateCephPrimaryStorageMonEvent();

        CephPrimaryStorageInventory ps = new CephPrimaryStorageInventory();
        ps.setName("My Ceph Primary Storage");
        ps.setDescription("Public Ceph Primary Storage");
        ps.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ps.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ps.setType("Ceph");
        CephPrimaryStorageMonInventory mon = new CephPrimaryStorageMonInventory();
        mon.setMonUuid(uuid());
        mon.setMonAddr("10.0.1.4");
        ps.setMons(Collections.singletonList(mon));
        ps.setState(PrimaryStorageState.Enabled.toString());
        ps.setStatus(PrimaryStorageStatus.Connected.toString());
        ps.setAvailableCapacity(924L * 1024L * 1024L);
        ps.setTotalCapacity(1024L * 1024L * 1024L);
        ps.setAttachedClusterUuids(Collections.singletonList(uuid()));

        event.setInventory(ps);

        return event;
    }

}

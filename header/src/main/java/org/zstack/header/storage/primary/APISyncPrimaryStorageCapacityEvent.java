package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

/**
 * Created by frank on 6/18/2015.
 */
@RestResponse(allTo = "inventory")
public class APISyncPrimaryStorageCapacityEvent extends APIEvent {
    private PrimaryStorageInventory inventory;

    public APISyncPrimaryStorageCapacityEvent() {
    }

    public APISyncPrimaryStorageCapacityEvent(String apiId) {
        super(apiId);
    }

    public PrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(PrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APISyncPrimaryStorageCapacityEvent __example__() {
        APISyncPrimaryStorageCapacityEvent event = new APISyncPrimaryStorageCapacityEvent();

        PrimaryStorageInventory ps = new PrimaryStorageInventory();
        ps.setName("PS1");
        ps.setUrl("/zstack_ps");
        ps.setType("LocalStorage");
        ps.setAttachedClusterUuids(Collections.singletonList(uuid()));
        ps.setState(PrimaryStorageState.Enabled.toString());
        ps.setStatus(PrimaryStorageStatus.Connected.toString());
        ps.setAvailableCapacity(1024L * 1024L * 928L);
        ps.setAvailablePhysicalCapacity(1024L * 1024L * 928L);

        event.setInventory(ps);
        return event;
    }

}

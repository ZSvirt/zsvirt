package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.primary.PrimaryStorageConstant;

/**
 * Created by frank on 6/15/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateDiskOfferingEvent extends APIEvent {
    private DiskOfferingInventory inventory;

    public APIUpdateDiskOfferingEvent() {
    }

    public APIUpdateDiskOfferingEvent(String apiId) {
        super(apiId);
    }

    public DiskOfferingInventory getInventory() {
        return inventory;
    }

    public void setInventory(DiskOfferingInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateDiskOfferingEvent __example__() {
        APIUpdateDiskOfferingEvent event = new APIUpdateDiskOfferingEvent();
        DiskOfferingInventory inventory = new DiskOfferingInventory();
        inventory.setName("new name");
        inventory.setDiskSize(100);
        inventory.setUuid(uuid());
        inventory.setAllocatorStrategy(PrimaryStorageConstant.DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE);
        inventory.setType("DefaultDiskOfferingType");
        inventory.setState(DiskOfferingState.Enabled.toString());

        event.setInventory(inventory);

        return event;
    }

}

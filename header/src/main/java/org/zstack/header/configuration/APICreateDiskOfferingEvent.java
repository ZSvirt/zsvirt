package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.primary.PrimaryStorageConstant;

@RestResponse(allTo = "inventory")
public class APICreateDiskOfferingEvent extends APIEvent {
    private DiskOfferingInventory inventory;

    public APICreateDiskOfferingEvent() {
        super(null);
    }

    public APICreateDiskOfferingEvent(String apiId) {
        super(apiId);
    }

    public DiskOfferingInventory getInventory() {
        return inventory;
    }

    public void setInventory(DiskOfferingInventory inventory) {
        this.inventory = inventory;
    }

 
    public static APICreateDiskOfferingEvent __example__() {
        APICreateDiskOfferingEvent event = new APICreateDiskOfferingEvent();
        DiskOfferingInventory inventory = new DiskOfferingInventory();
        inventory.setName("diskOffering1");
        inventory.setDiskSize(100);
        inventory.setUuid(uuid());
        inventory.setAllocatorStrategy(PrimaryStorageConstant.DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE);
        inventory.setType("DefaultDiskOfferingType");
        inventory.setState(DiskOfferingState.Enabled.toString());

        event.setInventory(inventory);
        return event;
    }

}

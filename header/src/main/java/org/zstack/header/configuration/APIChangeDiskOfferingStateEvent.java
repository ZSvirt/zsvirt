package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.primary.PrimaryStorageConstant;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(allTo = "inventory")
public class APIChangeDiskOfferingStateEvent extends APIEvent {
    private DiskOfferingInventory inventory;

    public APIChangeDiskOfferingStateEvent() {
        super(null);
    }

    public APIChangeDiskOfferingStateEvent(String apiId) {
        super(apiId);
    }

    public DiskOfferingInventory getInventory() {
        return inventory;
    }

    public void setInventory(DiskOfferingInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeDiskOfferingStateEvent __example__() {
        APIChangeDiskOfferingStateEvent event = new APIChangeDiskOfferingStateEvent();
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

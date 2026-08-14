package org.zstack.storage.primary.local;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 11/16/2015.
 */
@RestResponse(allTo = "inventory")
public class APILocalStorageMigrateVolumeEvent extends APIEvent {
    private LocalStorageResourceRefInventory inventory;

    public APILocalStorageMigrateVolumeEvent() {
    }

    public APILocalStorageMigrateVolumeEvent(String apiId) {
        super(apiId);
    }

    public LocalStorageResourceRefInventory getInventory() {
        return inventory;
    }

    public void setInventory(LocalStorageResourceRefInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APILocalStorageMigrateVolumeEvent __example__() {
        APILocalStorageMigrateVolumeEvent event = new APILocalStorageMigrateVolumeEvent();

        LocalStorageResourceRefInventory inv = new LocalStorageResourceRefInventory();
        inv.setResourceUuid(uuid());
        inv.setHostUuid(uuid());
        inv.setPrimaryStorageUuid(uuid());
        inv.setSize(1024L * 23472);

        event.setInventory(inv);
        return event;
    }

}

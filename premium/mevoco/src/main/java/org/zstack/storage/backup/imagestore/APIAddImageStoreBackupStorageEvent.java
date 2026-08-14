package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddImageStoreBackupStorageEvent extends APIEvent {

    private ImageStoreBackupStorageInventory inventory;

    public ImageStoreBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageStoreBackupStorageInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddImageStoreBackupStorageEvent __example__() {
        APIAddImageStoreBackupStorageEvent event = new APIAddImageStoreBackupStorageEvent();
        return event;
    }
}

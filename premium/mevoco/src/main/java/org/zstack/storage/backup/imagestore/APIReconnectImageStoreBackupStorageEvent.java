package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageStatus;

import java.util.Collections;

@RestResponse(allTo = "inventory")
public class APIReconnectImageStoreBackupStorageEvent extends APIEvent {
    private ImageStoreBackupStorageInventory inventory;

    public APIReconnectImageStoreBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APIReconnectImageStoreBackupStorageEvent() {
        super(null);
    }

    public ImageStoreBackupStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageStoreBackupStorageInventory inventory) {
        this.inventory = inventory;
    }

    public static APIReconnectImageStoreBackupStorageEvent __example__() {
        APIReconnectImageStoreBackupStorageEvent event = new APIReconnectImageStoreBackupStorageEvent();

        ImageStoreBackupStorageInventory inv = new ImageStoreBackupStorageInventory();
        inv.setName("ImageStore");
        inv.setState(BackupStorageState.Enabled.toString());
        inv.setStatus(BackupStorageStatus.Connected.toString());
        inv.setTotalCapacity(1024L * 1024L * 1024L);
        inv.setAvailableCapacity(768L * 1024L * 1024L);
        inv.setAttachedZoneUuids(Collections.singletonList(uuid()));

        event.setInventory(inv);
        return event;
    }

}

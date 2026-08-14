package org.zstack.header.storage.database.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;
@RestResponse(allTo = "inventory")
public class APISyncDatabaseBackupFromImageStoreBackupStorageEvent extends APIEvent {
    private DatabaseBackupInventory inventory;

    public APISyncDatabaseBackupFromImageStoreBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APISyncDatabaseBackupFromImageStoreBackupStorageEvent() {
        super(null);
    }

    public DatabaseBackupInventory getInventory() {
        return inventory;
    }

    public void setInventory(DatabaseBackupInventory inventory) {
        this.inventory = inventory;
    }

    public static APISyncDatabaseBackupFromImageStoreBackupStorageEvent __example__() {
        APISyncDatabaseBackupFromImageStoreBackupStorageEvent event = new APISyncDatabaseBackupFromImageStoreBackupStorageEvent();
        DatabaseBackupInventory inv = new DatabaseBackupInventory();
        inv.setUuid(uuid());
        inv.setName("Backup-1");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setDescription("database backup");
        inv.setSize(SizeUnit.MEGABYTE.toByte(1));

        event.setInventory(inv);

        return event;
    }
}
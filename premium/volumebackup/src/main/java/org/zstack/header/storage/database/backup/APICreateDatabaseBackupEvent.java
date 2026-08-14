package org.zstack.header.storage.database.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APICreateDatabaseBackupEvent extends APIEvent {
    private DatabaseBackupInventory inventory;

    public DatabaseBackupInventory getInventory() {
        return inventory;
    }

    public void setInventory(DatabaseBackupInventory inventory) {
        this.inventory = inventory;
    }


    public APICreateDatabaseBackupEvent(String apiId) {
        super(apiId);
    }

    public APICreateDatabaseBackupEvent() {
        super(null);
    }

    public static APICreateDatabaseBackupEvent __example__(){
        APICreateDatabaseBackupEvent evt = new APICreateDatabaseBackupEvent();
        DatabaseBackupInventory inventory = new DatabaseBackupInventory();
        inventory.setName("zsdb");
        inventory.setSize(1000L);
        inventory.setStatus(DatabaseBackupStatus.Ready.toString());
        inventory.setState(DatabaseBackupState.Enabled.toString());
        inventory.setMetadata("{\"version\":\"3.0.0\"}");
        inventory.setUuid(uuid(DatabaseBackupVO.class));
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        evt.setInventory(inventory);
        return evt;
    }
}

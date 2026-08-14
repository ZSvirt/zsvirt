package org.zstack.header.storage.database.backup;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryDatabaseBackupReply extends APIQueryReply {
    private List<DatabaseBackupInventory> inventories;

    public List<DatabaseBackupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<DatabaseBackupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryDatabaseBackupReply __example__() {
        APIQueryDatabaseBackupReply reply = new APIQueryDatabaseBackupReply();

        DatabaseBackupInventory inv = new DatabaseBackupInventory();
        inv.setUuid(uuid());
        inv.setName("db-backup");
        inv.setDescription("db-backup");
        inv.setSize(13107L);

        reply.setInventories(Collections.singletonList(inv));
        return reply;
    }
}

package org.zstack.storage.migration.backup;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.BackupStorageInventory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by GuoYi on 9/21/17.
 */
@RestResponse(allTo = "inventories")
public class APIGetBackupStorageCandidatesForImageMigrationReply extends APIReply {
    private List<BackupStorageInventory> inventories;

    public static APIGetBackupStorageCandidatesForImageMigrationReply __example__() {
        APIGetBackupStorageCandidatesForImageMigrationReply reply = new APIGetBackupStorageCandidatesForImageMigrationReply();
        BackupStorageInventory bsInv = new BackupStorageInventory();
        bsInv.setUuid(uuid());
        bsInv.setName("BS-1");
        reply.setInventories(asList(bsInv));
        return reply;
    }

    public List<BackupStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<BackupStorageInventory> inventories) {
        this.inventories = inventories;
    }
}

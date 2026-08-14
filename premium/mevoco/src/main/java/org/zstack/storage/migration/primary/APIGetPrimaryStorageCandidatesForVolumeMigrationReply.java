package org.zstack.storage.migration.primary;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.primary.PrimaryStorageInventory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by GuoYi on 9/21/17.
 */
@RestResponse(allTo = "inventories")
public class APIGetPrimaryStorageCandidatesForVolumeMigrationReply extends APIReply {
    private List<PrimaryStorageInventory> inventories;

    public static APIGetPrimaryStorageCandidatesForVolumeMigrationReply __example__() {
        APIGetPrimaryStorageCandidatesForVolumeMigrationReply reply = new APIGetPrimaryStorageCandidatesForVolumeMigrationReply();
        PrimaryStorageInventory psInv = new PrimaryStorageInventory();
        psInv.setUuid(uuid());
        psInv.setName("PS-1");
        reply.setInventories(asList(psInv));
        return reply;
    }

    public List<PrimaryStorageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PrimaryStorageInventory> inventories) {
        this.inventories = inventories;
    }
}

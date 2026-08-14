package org.zstack.header.storage.backup;

import org.zstack.header.core.trash.InstallPathRecycleInventory;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestResponse(allTo = "inventories")
public class APIGetTrashOnBackupStorageReply extends APIReply {
    private List<InstallPathRecycleInventory> inventories = new ArrayList<>();

    public List<InstallPathRecycleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<InstallPathRecycleInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetTrashOnBackupStorageReply __example__() {
        APIGetTrashOnBackupStorageReply reply = new APIGetTrashOnBackupStorageReply();

        InstallPathRecycleInventory inventory = new InstallPathRecycleInventory();
        inventory.setTrashId(1L);
        inventory.setTrashType("MigrateImage");
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setResourceUuid(uuid());
        inventory.setResourceType("ImageVO");
        inventory.setStorageUuid(uuid());
        inventory.setStorageType("BackupStorageVO");
        inventory.setInstallPath("/zstack_bs/installpath");
        inventory.setSize(1024000L);

        reply.setInventories(asList(inventory));

        return reply;
    }
}

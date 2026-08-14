package org.zstack.header.storage.volume.backup;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.VolumeBackupInventory;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryVolumeBackupReply extends APIQueryReply {
    private List<VolumeBackupInventory> inventories;

    public List<VolumeBackupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeBackupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVolumeBackupReply __example__() {
        APIQueryVolumeBackupReply reply = new APIQueryVolumeBackupReply();

        VolumeBackupInventory inv = new VolumeBackupInventory();
        inv.setUuid(uuid());
        inv.setName("backup-2");
        inv.setDescription("my backup");
        inv.setSize(1310720L);
        inv.setVolumeUuid(uuid());

        reply.setInventories(Collections.singletonList(inv));
        return reply;
    }
}

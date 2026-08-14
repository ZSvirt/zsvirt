package org.zstack.header.storage.primary;

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
public class APIGetTrashOnPrimaryStorageReply extends APIReply {
    private List<InstallPathRecycleInventory> inventories = new ArrayList<>();

    public List<InstallPathRecycleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<InstallPathRecycleInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetTrashOnPrimaryStorageReply __example__() {
        APIGetTrashOnPrimaryStorageReply reply = new APIGetTrashOnPrimaryStorageReply();

        InstallPathRecycleInventory inv1 = new InstallPathRecycleInventory();
        inv1.setTrashId(1L);
        inv1.setTrashType("MigrateVolume");
        inv1.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv1.setResourceUuid(uuid());
        inv1.setResourceType("VolumeVO");
        inv1.setStorageUuid(uuid());
        inv1.setStorageType("PrimaryStorageVO");
        inv1.setInstallPath("/zstack_ps/installpath");
        inv1.setSize(1024000L);

        InstallPathRecycleInventory inv2 = new InstallPathRecycleInventory();
        inv2.setTrashId(2L);
        inv2.setTrashType("MigrateVolumeSnapshot");
        inv2.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv2.setResourceUuid(uuid());
        inv2.setResourceType("VolumeSnapshotVO");
        inv2.setStorageUuid(uuid());
        inv2.setStorageType("PrimaryStorageVO");
        inv2.setInstallPath("/zstack_ps/installpath/snapshot");
        inv2.setSize(1024000L);

        reply.setInventories(asList(inv1, inv2));

        return reply;
    }
}
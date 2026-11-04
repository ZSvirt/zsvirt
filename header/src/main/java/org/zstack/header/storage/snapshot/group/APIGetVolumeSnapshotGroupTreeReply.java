package org.zstack.header.storage.snapshot.group;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIGetVolumeSnapshotGroupTreeReply extends APIReply {
    private List<VolumeSnapshotGroupTreeInventory> inventories;

    public List<VolumeSnapshotGroupTreeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotGroupTreeInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVolumeSnapshotGroupTreeReply __example__() {
        VolumeSnapshotGroupTreeInventory inv = new VolumeSnapshotGroupTreeInventory();
        inv.setUuid(uuid());
        inv.setName("group");
        inv.setVmInstanceUuid(uuid());
        inv.setCurrent(true);
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        APIGetVolumeSnapshotGroupTreeReply reply = new APIGetVolumeSnapshotGroupTreeReply();
        reply.setInventories(Collections.singletonList(inv));
        return reply;
    }
}

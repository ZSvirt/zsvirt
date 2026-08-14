package org.zstack.header.storage.snapshot.group;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.volume.VolumeType;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/7/11.
 */
@RestResponse(allTo = "inventories")
public class APIQueryVolumeSnapshotGroupReply extends APIQueryReply {
    private List<VolumeSnapshotGroupInventory> inventories;

    public List<VolumeSnapshotGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVolumeSnapshotGroupReply __example__() {
        VolumeSnapshotGroupInventory inv = new VolumeSnapshotGroupInventory();
        inv.setName("group");
        inv.setSnapshotCount(1);
        inv.setUuid(uuid());
        inv.setVmInstanceUuid(uuid());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        VolumeSnapshotGroupRefInventory ref = new VolumeSnapshotGroupRefInventory();
        ref.setDeviceId(0);
        ref.setSnapshotDeleted(false);
        ref.setVolumeName("ROOT-volume");
        ref.setVolumeUuid(uuid());
        ref.setVolumeSnapshotInstallPath("/zstack_ps/to/path/snap.qcow2");
        ref.setVolumeSnapshotUuid(uuid());
        ref.setVolumeSnapshotName("group-ROOT-volume");
        ref.setVolumeSnapshotGroupUuid(inv.getUuid());
        ref.setVolumeType(VolumeType.Root.toString());
        ref.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ref.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setVolumeSnapshotRefs(Collections.singletonList(ref));

        APIQueryVolumeSnapshotGroupReply result = new APIQueryVolumeSnapshotGroupReply();
        result.inventories = Collections.singletonList(inv);
        return result;
    }
}

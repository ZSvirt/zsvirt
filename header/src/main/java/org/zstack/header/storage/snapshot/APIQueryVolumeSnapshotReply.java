package org.zstack.header.storage.snapshot;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.volume.VolumeType;

import java.util.Collections;
import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryVolumeSnapshotReply extends APIQueryReply {
    private List<VolumeSnapshotInventory> inventories;

    public List<VolumeSnapshotInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryVolumeSnapshotReply __example__() {
        APIQueryVolumeSnapshotReply reply = new APIQueryVolumeSnapshotReply();

        VolumeSnapshotInventory inv = new VolumeSnapshotInventory();
        inv.setUuid(uuid());
        inv.setName("My Snapshot 2");
        inv.setPrimaryStorageUuid(uuid());
        inv.setFormat("qcow2");
        inv.setLatest(false);
        inv.setPrimaryStorageUuid("/zstack_ps/rootVolumes/acct-e77f16d460ea46e18262547b56972273/vol-13c66bb52d0949398e520183b917f813/snapshots/2fa6979af5c6479fa98f37d316f44b5f.qcow2");
        inv.setSize(1310720);
        inv.setStatus(VolumeSnapshotStatus.Ready.toString());
        inv.setState(VolumeSnapshotState.Enabled.toString());
        inv.setVolumeType(VolumeType.Root.toString());

        reply.setInventories(Collections.singletonList(inv));
        return reply;
    }

}

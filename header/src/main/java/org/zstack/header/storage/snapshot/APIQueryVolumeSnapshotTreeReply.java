package org.zstack.header.storage.snapshot;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.volume.VolumeType;

import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryVolumeSnapshotTreeReply extends APIQueryReply {
    private List<VolumeSnapshotTreeInventory> inventories;

    public List<VolumeSnapshotTreeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotTreeInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryVolumeSnapshotTreeReply __example__() {
        APIQueryVolumeSnapshotTreeReply reply = new APIQueryVolumeSnapshotTreeReply();

        VolumeSnapshotInventory inv = new VolumeSnapshotInventory();
        inv.setUuid(uuid());
        inv.setName("My Snapshot 2");
        inv.setPrimaryStorageUuid(uuid());
        inv.setFormat("qcow2");
        inv.setVolumeUuid(uuid());
        inv.setLatest(false);
        inv.setPrimaryStorageUuid("/zstack_ps/rootVolumes/acct-e77f16d460ea46e18262547b56972273/vol-13c66bb52d0949398e520183b917f813/snapshots/2fa6979af5c6479fa98f37d316f44b5f.qcow2");
        inv.setSize(1310720);
        inv.setStatus(VolumeSnapshotStatus.Ready.toString());
        inv.setState(VolumeSnapshotState.Enabled.toString());
        inv.setVolumeType(VolumeType.Root.toString());

        VolumeSnapshotTree.SnapshotLeafInventory linv = new VolumeSnapshotTree.SnapshotLeafInventory();
        linv.setInventory(inv);
        linv.setParentUuid(uuid());

        VolumeSnapshotTreeInventory tinv = new VolumeSnapshotTreeInventory();
        tinv.setUuid(uuid());
        tinv.setCurrent(false);
        tinv.setVolumeUuid(inv.getVolumeUuid());
        tinv.setTree(linv);
        tinv.setStatus(VolumeSnapshotTreeStatus.Completed.toString());

        return reply;
    }

}

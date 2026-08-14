package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Create by weiwang at 2018/6/8
 */
@RestResponse(allTo = "inventories")
public class APICreateVolumesSnapshotEvent extends APIEvent {
    private List<VolumeSnapshotInventory> inventories;

    public APICreateVolumesSnapshotEvent(String apiId) {
        super(apiId);
    }

    public APICreateVolumesSnapshotEvent() {
    }

    public List<VolumeSnapshotInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotInventory> inventories) {
        this.inventories = inventories;
    }

    public static APICreateVolumesSnapshotEvent __example__() {
        APICreateVolumesSnapshotEvent event = new APICreateVolumesSnapshotEvent();
        String volumeUuid= uuid();
        String snapshotUuid = uuid();
        VolumeSnapshotInventory inv1 = new VolumeSnapshotInventory();
        inv1.setName("Snapshot-1");
        inv1.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv1.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv1.setParentUuid(uuid());
        inv1.setDescription("create-snapshot-from-volume");
        inv1.setState(VolumeState.Enabled.toString());
        inv1.setType("Hypervisor");
        inv1.setVolumeUuid(volumeUuid);
        inv1.setFormat("qcow2");
        inv1.setUuid(snapshotUuid);
        inv1.setStatus("Ready");
        inv1.setPrimaryStorageUuid(uuid());
        inv1.setPrimaryStorageInstallPath(String.format("/zstack_ps/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-%s/snapshots/%s.qcow2", volumeUuid, snapshotUuid));
        inv1.setLatest(true);
        inv1.setSize(SizeUnit.GIGABYTE.toByte(1));
        inv1.setVolumeType(VolumeType.Root.toString());
        inv1.setTreeUuid(uuid());

        VolumeSnapshotInventory inv2 = new VolumeSnapshotInventory();
        inv2.setName("Snapshot-2");
        inv2.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv2.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv2.setParentUuid(uuid());
        inv2.setDescription("create-snapshot-from-volume");
        inv2.setState(VolumeState.Enabled.toString());
        inv2.setType("Hypervisor");
        inv2.setVolumeUuid(volumeUuid);
        inv2.setFormat("qcow2");
        inv2.setUuid(snapshotUuid);
        inv2.setStatus("Ready");
        inv2.setPrimaryStorageUuid(uuid());
        inv2.setPrimaryStorageInstallPath(String.format("/zstack_ps/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-%s/snapshots/%s.qcow2", volumeUuid, snapshotUuid));
        inv2.setLatest(true);
        inv2.setSize(SizeUnit.GIGABYTE.toByte(1));
        inv2.setVolumeType(VolumeType.Root.toString());
        inv2.setTreeUuid(uuid());

        event.setInventories(Arrays.asList(inv1, inv2));

        return event;
    }
}

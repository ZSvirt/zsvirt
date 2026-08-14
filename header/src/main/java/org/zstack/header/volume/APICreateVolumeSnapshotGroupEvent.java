package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefInventory;

import java.sql.Timestamp;
import java.util.Collections;

/**
 * Created by MaJin on 2019/7/9.
 */
@RestResponse(allTo = "inventory")
public class APICreateVolumeSnapshotGroupEvent extends APIEvent {
    private VolumeSnapshotGroupInventory inventory;

    public VolumeSnapshotGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeSnapshotGroupInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateVolumeSnapshotGroupEvent(String apiId) {
        super(apiId);
    }

    public APICreateVolumeSnapshotGroupEvent() {
        super();
    }

    public static APICreateVolumeSnapshotGroupEvent __example__() {
        APICreateVolumeSnapshotGroupEvent event = new APICreateVolumeSnapshotGroupEvent(uuid());
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
        event.inventory = inv;
        return event;
    }
}

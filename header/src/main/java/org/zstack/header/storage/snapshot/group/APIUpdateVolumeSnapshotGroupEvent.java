package org.zstack.header.storage.snapshot.group;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.volume.VolumeType;

import java.sql.Timestamp;
import java.util.Collections;

/**
 * Created by MaJin on 2019/8/29.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateVolumeSnapshotGroupEvent extends APIEvent {
    private VolumeSnapshotGroupInventory inventory;

    public APIUpdateVolumeSnapshotGroupEvent() {
        super();
    }

    public APIUpdateVolumeSnapshotGroupEvent(String apiId) {
        super(apiId);
    }

    public VolumeSnapshotGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeSnapshotGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateVolumeSnapshotGroupEvent __example__() {
        APIUpdateVolumeSnapshotGroupEvent event = new APIUpdateVolumeSnapshotGroupEvent(APIMessage.uuid());

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

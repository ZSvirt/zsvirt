package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.snapshot.group.APIQueryVolumeSnapshotGroupReply;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefInventory;
import org.zstack.header.volume.VolumeType;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

/**
 * Created by LiangHanYu on 2022/7/5 17:45
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetMemorySnapshotGroupReferenceReply extends APIReply {
    private List<VolumeSnapshotGroupInventory> inventories;
    private String resourceUuid;

    public APIGetMemorySnapshotGroupReferenceReply(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public APIGetMemorySnapshotGroupReferenceReply() {
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public List<VolumeSnapshotGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeSnapshotGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetMemorySnapshotGroupReferenceReply __example__() {
        APIGetMemorySnapshotGroupReferenceReply reply = new APIGetMemorySnapshotGroupReferenceReply();
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
        reply.setResourceUuid(uuid());
        reply.setInventories(Collections.singletonList(inv));
        return reply;
    }

}

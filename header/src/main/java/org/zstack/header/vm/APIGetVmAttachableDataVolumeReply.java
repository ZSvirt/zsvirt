package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;
import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIGetVmAttachableDataVolumeReply extends APIReply {
    private List<VolumeInventory> inventories;

    public List<VolumeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIGetVmAttachableDataVolumeReply __example__() {
        APIGetVmAttachableDataVolumeReply reply = new APIGetVmAttachableDataVolumeReply();

        VolumeInventory vol = new VolumeInventory();
        vol.setName("data");
        vol.setUuid("4b9fb654ff6a4e33bf7150de276cc1a4");
        vol.setPrimaryStorageUuid(uuid());
        vol.setStatus(VolumeStatus.Ready.toString());
        vol.setInstallPath("/zstack_ps/dataVolumes/acct-603d42007c5e4722919f729813763359/vol-4b9fb654ff6a4e33bf7150de276cc1a4/4b9fb654ff6a4e33bf7150de276cc1a4.qcow2");
        vol.setActualSize(SizeUnit.GIGABYTE.toByte(10));
        vol.setSize(SizeUnit.GIGABYTE.toByte(100));
        vol.setFormat("qcow2");
        vol.setState(VolumeState.Enabled.toString());
        vol.setDiskOfferingUuid(uuid());
        vol.setType(VolumeType.Data.toString());
        vol.setShareable(false);
        vol.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vol.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        return reply;
    }

}

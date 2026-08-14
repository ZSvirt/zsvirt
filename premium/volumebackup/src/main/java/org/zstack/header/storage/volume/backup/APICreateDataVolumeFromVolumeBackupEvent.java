package org.zstack.header.storage.volume.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APICreateDataVolumeFromVolumeBackupEvent extends APIEvent {
    private VolumeInventory inventory;

    public APICreateDataVolumeFromVolumeBackupEvent(String apiId) {
        super(apiId);
    }

    public APICreateDataVolumeFromVolumeBackupEvent() {
        super(null);
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateDataVolumeFromVolumeBackupEvent __example__() {
        APICreateDataVolumeFromVolumeBackupEvent evt = new APICreateDataVolumeFromVolumeBackupEvent();

        VolumeInventory volumeInventory = new VolumeInventory();
        volumeInventory.setName("data-volume-from-backup");
        volumeInventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        volumeInventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        volumeInventory.setType(VolumeType.Data.toString());
        volumeInventory.setUuid(uuid());
        volumeInventory.setSize(SizeUnit.GIGABYTE.toByte(100));
        volumeInventory.setActualSize(SizeUnit.GIGABYTE.toByte(20));
        volumeInventory.setDeviceId(1);
        volumeInventory.setState(VolumeState.Enabled.toString());
        volumeInventory.setFormat("qcow2");
        volumeInventory.setInstallPath(String.format("/zstack_ps/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-%s/%s.qcow2", uuid(), volumeInventory.getUuid()));
        volumeInventory.setStatus(VolumeStatus.Ready.toString());
        volumeInventory.setPrimaryStorageUuid(uuid());
        volumeInventory.setVmInstanceUuid(uuid());

        evt.setInventory(volumeInventory);

        return evt;
    }
}

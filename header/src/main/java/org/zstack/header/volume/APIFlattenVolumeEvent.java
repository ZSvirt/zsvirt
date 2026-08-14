package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIFlattenVolumeEvent extends APIEvent {
    private VolumeInventory inventory;

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public APIFlattenVolumeEvent() {
        super();
    }

    public APIFlattenVolumeEvent(String id) {
        super(id);
    }

    public static APIFlattenVolumeEvent __example__() {
        APIFlattenVolumeEvent event = new APIFlattenVolumeEvent();

        String volumeUuid = uuid();
        VolumeInventory vol = new VolumeInventory();
        vol.setName("test-volume");
        vol.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vol.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vol.setType(VolumeType.Root.toString());
        vol.setUuid(volumeUuid);
        vol.setSize(SizeUnit.GIGABYTE.toByte(100));
        vol.setActualSize(SizeUnit.GIGABYTE.toByte(20));
        vol.setDeviceId(0);
        vol.setState(VolumeState.Enabled.toString());
        vol.setFormat("qcow2");
        vol.setDiskOfferingUuid(uuid());
        vol.setInstallPath(String.format("/zstack_ps/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-%s/%s.qcow2", volumeUuid, volumeUuid));
        vol.setStatus(VolumeStatus.Ready.toString());
        vol.setPrimaryStorageUuid(uuid());
        vol.setVmInstanceUuid(uuid());
        vol.setRootImageUuid(uuid());

        event.setInventory(vol);
        return event;
    }
}

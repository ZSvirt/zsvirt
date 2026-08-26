package org.zstack.header.volume;

import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.data.SizeUnit;

@RestResponse(allTo = "inventory")
public class APIChangeVolumeEncryptionEvent extends APIEvent {
    private VolumeInventory inventory;

    public APIChangeVolumeEncryptionEvent() {
    }

    public APIChangeVolumeEncryptionEvent(String apiId) {
        super(apiId);
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeVolumeEncryptionEvent __example__() {
        APIChangeVolumeEncryptionEvent event = new APIChangeVolumeEncryptionEvent();

        String volumeUuid = uuid(VolumeVO.class);
        VolumeInventory vol = new VolumeInventory();
        vol.setName("test-volume");
        vol.setCreateDate(DocUtils.timestamp());
        vol.setLastOpDate(DocUtils.timestamp());
        vol.setType(VolumeType.Root.toString());
        vol.setUuid(volumeUuid);
        vol.setSize(SizeUnit.GIGABYTE.toByte(100));
        vol.setActualSize(SizeUnit.GIGABYTE.toByte(20));
        vol.setDeviceId(0);
        vol.setState(VolumeState.Enabled.toString());
        vol.setFormat("qcow2");
        vol.setDiskOfferingUuid(uuid(DiskOfferingVO.class));
        vol.setInstallPath(String.format("/zstack_ps/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-%s/%s.qcow2", volumeUuid, volumeUuid));
        vol.setStatus(VolumeStatus.Ready.toString());
        vol.setPrimaryStorageUuid(uuid(PrimaryStorageVO.class));
        vol.setVmInstanceUuid(uuid(VmInstanceVO.class));
        vol.setRootImageUuid(uuid(ImageVO.class));
        vol.setEncrypted(true);

        event.setInventory(vol);
        return event;
    }
}

package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;

/**
 * @apiResult api event for message :ref:`APIChangeVolumeStateMsg`
 * @example {
 * "org.zstack.header.volume.APICreateDataVolumeEvent": {
 * "inventory": {
 * "uuid": "f035366497994ef6bda20a45c4b3ee2e",
 * "name": "TestData",
 * "type": "Data",
 * "size": 10737418240,
 * "state": "Enabled",
 * "status": "NotInstantiated",
 * "createDate": "May 2, 2014 8:07:29 PM",
 * "lastOpDate": "May 2, 2014 8:07:29 PM",
 * "backupStorageRefs": []
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APICreateDataVolumeEvent extends APIEvent {
    public APICreateDataVolumeEvent(String apiId) {
        super(apiId);
    }

    public APICreateDataVolumeEvent() {
        super(null);
    }

    /**
     * @desc see :ref:`VolumeInventory`
     */
    private VolumeInventory inventory;

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateDataVolumeEvent __example__() {
        APICreateDataVolumeEvent event = new APICreateDataVolumeEvent();

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

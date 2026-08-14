package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for message :ref:`APIBackupDataVolumeMsg`
 * @example {
 * "org.zstack.header.volume.APIBackupDataVolumeEvent": {
 * "inventory": {
 * "uuid": "d4910ee8def241e7afcb55ca1ee685c9",
 * "name": "d1",
 * "primaryStorageUuid": "29ea91d6cfb544a392b24f84a43de154",
 * "vmInstanceUuid": "0135cb45094f4f6fb84375e13d4a1cb8",
 * "installPath": "/opt/zstack/nfsprimarystorage/prim-29ea91d6cfb544a392b24f84a43de154/dataVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-d4910ee8def241e7afcb55ca1ee685c9/d4910ee8def241e7afcb55ca1ee685c9.qcow2",
 * "type": "Data",
 * "hypervisorType": "KVM",
 * "size": 32212254720,
 * "deviceId": 1,
 * "state": "Enabled",
 * "status": "Ready",
 * "createDate": "May 2, 2014 7:55:15 PM",
 * "lastOpDate": "May 2, 2014 7:55:15 PM",
 * "backupStorageRefs": [
 * {
 * "volumeUuid": "d4910ee8def241e7afcb55ca1ee685c9",
 * "backupStorageUuid": "e028f12592fa40359b9af5b8946b1c53",
 * "installPath": "nfs:/test1/volumeSnapshots/acct-36c27e8ff05c4780bf6d2fa65700f22e/d4910ee8def241e7afcb55ca1ee685c9/d4910ee8def241e7afcb55ca1ee685c9.qcow2"
 * }
 * ]
 * },
 * "success": true
 * }
 * }
 * @since 0.1.0
 */
@Deprecated
public class APIBackupDataVolumeEvent extends APIEvent {
    /**
     * @desc see :ref:`VolumeInventory`
     */
    private VolumeInventory inventory;

    public APIBackupDataVolumeEvent(String apiId) {
        super(apiId);
    }

    public APIBackupDataVolumeEvent() {
        super(null);
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIBackupDataVolumeEvent __example__() {
        APIBackupDataVolumeEvent event = new APIBackupDataVolumeEvent();


        return event;
    }

}

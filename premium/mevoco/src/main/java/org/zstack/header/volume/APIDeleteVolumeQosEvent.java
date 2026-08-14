package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 17/1/11.
 */
@RestResponse(allTo = "inventory")
public class APIDeleteVolumeQosEvent extends APIEvent {
    private VolumeInventory inventory;

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }

    public APIDeleteVolumeQosEvent() {
        super(null);
    }

    public APIDeleteVolumeQosEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeleteVolumeQosEvent __example__() {
        APIDeleteVolumeQosEvent event = new APIDeleteVolumeQosEvent();

        String uuid = uuid();
        VolumeInventory inventory = new VolumeInventory();
        inventory.setName(String.format("volume"));
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setType(VolumeType.Root.toString());
        inventory.setUuid(uuid);
        inventory.setSize(SizeUnit.GIGABYTE.toByte(100));
        inventory.setActualSize(SizeUnit.GIGABYTE.toByte(20));
        inventory.setDeviceId(0);
        inventory.setState(VolumeState.Enabled.toString());
        inventory.setFormat("qcow2");
        inventory.setDiskOfferingUuid(uuid());
        inventory.setInstallPath(String.format("/zstack_ps/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/inventory-%s/%s.qcow2", uuid, uuid));
        inventory.setStatus(VolumeStatus.Ready.toString());
        inventory.setPrimaryStorageUuid(uuid());
        inventory.setVmInstanceUuid(uuid());
        inventory.setRootImageUuid(uuid());
        inventory.setVolumeQos("write=20971520");

        return event;
    }

}

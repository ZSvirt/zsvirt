package org.zstack.header.vm.devices;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by LiangHanYu on 2022/6/20 18:03
 */
@RestResponse(allTo = "inventories")
public class APIQueryVmInstanceResourceMetadataGroupReply extends APIQueryReply {
    private List<VmInstanceResourceMetadataGroupInventory> inventories;

    public List<VmInstanceResourceMetadataGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmInstanceResourceMetadataGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmInstanceResourceMetadataGroupReply __example__() {
        VmInstanceResourceMetadataGroupInventory inv = new VmInstanceResourceMetadataGroupInventory();
        inv.setUuid(uuid());
        inv.setResourceUuid(uuid());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        VmInstanceResourceMetadataArchiveInventory archiveInventory = new VmInstanceResourceMetadataArchiveInventory();
        archiveInventory.setId(1);
        archiveInventory.setResourceUuid(uuid());

        DeviceAddress address = new DeviceAddress();
        address.bus = "00";
        address.domain = "0000";
        address.slot = "0d";
        address.function = "0";

        archiveInventory.setDeviceAddress(address.toString());
        archiveInventory.setAddressGroupUuid(inv.getUuid());
        archiveInventory.setVmInstanceUuid(uuid());
        archiveInventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        archiveInventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        String vmUuid = uuid();
        VolumeInventory vol = new VolumeInventory();
        vol.setName(String.format("Root-Volume-For-VM-%s", vmUuid));
        vol.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vol.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vol.setType(VolumeType.Root.toString());
        vol.setUuid(uuid());
        vol.setSize(SizeUnit.GIGABYTE.toByte(100));
        vol.setActualSize(SizeUnit.GIGABYTE.toByte(20));
        vol.setDeviceId(0);
        vol.setState(VolumeState.Enabled.toString());
        vol.setFormat("qcow2");
        vol.setDiskOfferingUuid(uuid());
        vol.setInstallPath(String.format("/zstack_ps/rootVolumes/acct-36c27e8ff05c4780bf6d2fa65700f22e/vol-%s/%s.qcow2", vol.getUuid(), vol.getUuid()));
        vol.setStatus(VolumeStatus.Ready.toString());
        vol.setPrimaryStorageUuid(uuid());
        vol.setVmInstanceUuid(vmUuid);
        vol.setRootImageUuid(uuid());

        archiveInventory.setMetadata(JSONObjectUtil.toJsonString(vol));
        archiveInventory.setMetadataClass(VmInstanceResourceMetadataGroupInventory.class.getCanonicalName());
        inv.setAddressList(Arrays.asList(archiveInventory));
        APIQueryVmInstanceResourceMetadataGroupReply result = new APIQueryVmInstanceResourceMetadataGroupReply();
        result.inventories = Collections.singletonList(inv);
        return result;
    }
}

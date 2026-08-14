package org.zstack.header.vm.devices;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

/**
 * Created by LiangHanYu on 2022/6/17 17:31
 */
@RestResponse(allTo = "inventories")
public class APIQueryVmInstanceResourceMetadataArchiveReply extends APIQueryReply {
    private List<VmInstanceResourceMetadataArchiveInventory> inventories;

    public List<VmInstanceResourceMetadataArchiveInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmInstanceResourceMetadataArchiveInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmInstanceResourceMetadataArchiveReply __example__() {
        VmInstanceResourceMetadataArchiveInventory inv = new VmInstanceResourceMetadataArchiveInventory();
        inv.setId(1);
        inv.setResourceUuid(uuid());

        DeviceAddress address = new DeviceAddress();
        address.bus = "00";
        address.domain = "0000";
        address.slot = "0d";
        address.function = "0";

        inv.setDeviceAddress(address.toString());
        inv.setAddressGroupUuid(uuid());
        inv.setMetadata("Metadata");
        inv.setMetadataClass(VmInstanceResourceMetadataArchiveInventory.class.getCanonicalName());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        APIQueryVmInstanceResourceMetadataArchiveReply result = new APIQueryVmInstanceResourceMetadataArchiveReply();
        result.inventories = Collections.singletonList(inv);
        return result;
    }
}

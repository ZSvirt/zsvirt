package org.zstack.mevoco;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryShareableVolumeVmInstanceRefReply extends APIQueryReply {
    private List<ShareableVolumeVmInstanceRefInventory> inventories;

    public List<ShareableVolumeVmInstanceRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ShareableVolumeVmInstanceRefInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryShareableVolumeVmInstanceRefReply __example__() {
        APIQueryShareableVolumeVmInstanceRefReply reply = new APIQueryShareableVolumeVmInstanceRefReply();
        ShareableVolumeVmInstanceRefInventory inventorie1 = new ShareableVolumeVmInstanceRefInventory();
        inventorie1.setUuid(uuid());
        inventorie1.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventorie1.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventorie1.setDeviceId(1);
        inventorie1.setVmInstanceUuid(uuid());
        inventorie1.setVolumeUuid(uuid());
        List<ShareableVolumeVmInstanceRefInventory> inventories = new ArrayList<>() ;
        inventories.add(inventorie1);
        return reply;
    }

}

package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-05-05.
 */
@RestResponse(allTo = "inventories")
public class APIGetMdevDeviceCandidatesReply extends APIReply {
    private List<MdevDeviceInventory> inventories;

    public List<MdevDeviceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MdevDeviceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetMdevDeviceCandidatesReply __example__() {
        APIGetMdevDeviceCandidatesReply rly = new APIGetMdevDeviceCandidatesReply();
        rly.setInventories(Collections.singletonList(MdevDeviceInventory.__example__()));
        return rly;
    }
}

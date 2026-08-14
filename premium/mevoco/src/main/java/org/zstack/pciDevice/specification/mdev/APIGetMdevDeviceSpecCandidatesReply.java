package org.zstack.pciDevice.specification.mdev;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-05-05.
 */
@RestResponse(allTo = "inventories")
public class APIGetMdevDeviceSpecCandidatesReply extends APIReply {
    private List<MdevDeviceSpecInventory> inventories;

    public List<MdevDeviceSpecInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MdevDeviceSpecInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetMdevDeviceSpecCandidatesReply __example__() {
        APIGetMdevDeviceSpecCandidatesReply rly = new APIGetMdevDeviceSpecCandidatesReply();
        rly.setInventories(Collections.singletonList(MdevDeviceSpecInventory.__example__()));
        return rly;
    }
}

package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-04-18.
 */
@RestResponse(allTo = "inventories")
public class APIQueryMdevDeviceReply extends APIQueryReply {
    private List<MdevDeviceInventory> inventories;

    public List<MdevDeviceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MdevDeviceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryMdevDeviceReply __example__() {
        APIQueryMdevDeviceReply reply = new APIQueryMdevDeviceReply();
        reply.setInventories(Collections.singletonList(MdevDeviceInventory.__example__()));
        return reply;
    }
}

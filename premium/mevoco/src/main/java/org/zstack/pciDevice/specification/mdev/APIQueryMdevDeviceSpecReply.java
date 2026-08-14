package org.zstack.pciDevice.specification.mdev;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-04-30.
 */
@RestResponse(allTo = "inventories")
public class APIQueryMdevDeviceSpecReply extends APIQueryReply {
    private List<MdevDeviceSpecInventory> inventories;

    public List<MdevDeviceSpecInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MdevDeviceSpecInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryMdevDeviceSpecReply __example__() {
        APIQueryMdevDeviceSpecReply reply = new APIQueryMdevDeviceSpecReply();
        reply.setInventories(Collections.singletonList(MdevDeviceSpecInventory.__example__()));
        return reply;
    }
}

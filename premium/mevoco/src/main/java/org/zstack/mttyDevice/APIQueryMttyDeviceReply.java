package org.zstack.mttyDevice;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * @author yu.sun
 * @date 2022/12/07 18:44
 **/
@RestResponse(allTo = "inventories")
public class APIQueryMttyDeviceReply extends APIQueryReply {
    private List<MttyDeviceInventory> inventories;

    public List<MttyDeviceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<MttyDeviceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryMttyDeviceReply __example__() {
        APIQueryMttyDeviceReply reply = new APIQueryMttyDeviceReply();
        reply.setInventories(Collections.singletonList(MttyDeviceInventory.__example__()));
        return reply;
    }
}
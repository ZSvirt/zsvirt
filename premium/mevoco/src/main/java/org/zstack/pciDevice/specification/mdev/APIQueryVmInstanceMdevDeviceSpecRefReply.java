package org.zstack.pciDevice.specification.mdev;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-05-22.
 */
@RestResponse(allTo = "inventories")
public class APIQueryVmInstanceMdevDeviceSpecRefReply extends APIQueryReply {
    private List<VmInstanceMdevDeviceSpecRefInventory> inventories;

    public List<VmInstanceMdevDeviceSpecRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmInstanceMdevDeviceSpecRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmInstanceMdevDeviceSpecRefReply __example__() {
        APIQueryVmInstanceMdevDeviceSpecRefReply reply = new APIQueryVmInstanceMdevDeviceSpecRefReply();
        reply.setInventories(Collections.singletonList(VmInstanceMdevDeviceSpecRefInventory.__example__()));
        return reply;
    }
}

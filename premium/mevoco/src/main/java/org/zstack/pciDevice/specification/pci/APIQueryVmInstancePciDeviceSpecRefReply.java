package org.zstack.pciDevice.specification.pci;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-05-22.
 */
@RestResponse(allTo = "inventories")
public class APIQueryVmInstancePciDeviceSpecRefReply extends APIQueryReply {
    private List<VmInstancePciDeviceSpecRefInventory> inventories;

    public List<VmInstancePciDeviceSpecRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmInstancePciDeviceSpecRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmInstancePciDeviceSpecRefReply __example__() {
        APIQueryVmInstancePciDeviceSpecRefReply reply = new APIQueryVmInstancePciDeviceSpecRefReply();
        reply.setInventories(Collections.singletonList(VmInstancePciDeviceSpecRefInventory.__example__()));
        return reply;
    }
}

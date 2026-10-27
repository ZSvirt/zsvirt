package org.zstack.pciDevice;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.pciDevice.entity.PciDeviceInventory;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by weiwang on 02/09/2017
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetPciDeviceCandidatesForNewCreateVmReply extends APIReply {
    private List<PciDeviceInventory> inventories;

    public List<PciDeviceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PciDeviceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetPciDeviceCandidatesForNewCreateVmReply __example__() {
        APIGetPciDeviceCandidatesForNewCreateVmReply reply = new APIGetPciDeviceCandidatesForNewCreateVmReply();
        reply.setInventories(list(PciDeviceInventory.__example__()));
        return reply;
    }
}

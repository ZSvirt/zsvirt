package org.zstack.pciDevice.specification.pci;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-03-13.
 */
@RestResponse(allTo = "inventories")
public class APIGetPciDeviceSpecCandidatesReply extends APIReply {
    private List<PciDeviceSpecInventory> inventories;

    public List<PciDeviceSpecInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PciDeviceSpecInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetPciDeviceSpecCandidatesReply __example__() {
        APIGetPciDeviceSpecCandidatesReply rly = new APIGetPciDeviceSpecCandidatesReply();
        rly.setInventories(Collections.singletonList(PciDeviceSpecInventory.__example__()));
        return rly;
    }
}

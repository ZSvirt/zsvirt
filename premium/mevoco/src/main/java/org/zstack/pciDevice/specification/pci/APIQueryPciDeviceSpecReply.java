package org.zstack.pciDevice.specification.pci;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-03-06.
 */
@RestResponse(allTo = "inventories")
public class APIQueryPciDeviceSpecReply extends APIQueryReply {
    private List<PciDeviceSpecInventory> inventories;

    public List<PciDeviceSpecInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<PciDeviceSpecInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryPciDeviceSpecReply __example__() {
        APIQueryPciDeviceSpecReply rly = new APIQueryPciDeviceSpecReply();
        rly.setInventories(Collections.singletonList(PciDeviceSpecInventory.__example__()));
        return rly;
    }
}

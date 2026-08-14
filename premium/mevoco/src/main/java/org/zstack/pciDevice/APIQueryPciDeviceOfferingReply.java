package org.zstack.pciDevice;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by weiwang on 17/06/2017.
 */
@RestResponse(allTo = "inventories")
public class APIQueryPciDeviceOfferingReply extends APIQueryReply {
    private List<PciDeviceOfferingInventory> inventories;

    public List<PciDeviceOfferingInventory>  getInventories() {
        return inventories;
    }

    public void setInventories(List<PciDeviceOfferingInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryPciDeviceOfferingReply __example__() {
        APIQueryPciDeviceOfferingReply reply = new APIQueryPciDeviceOfferingReply();
        PciDeviceOfferingInventory inv = new PciDeviceOfferingInventory();

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}

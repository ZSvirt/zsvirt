package org.zstack.pciDevice;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by weiwang on 17/06/2017.
 */
@RestResponse(allTo = "inventories")
public class APIQueryPciDeviceReply extends APIQueryReply {
    private List<PciDeviceInventory> inventories;

    public List<PciDeviceInventory>  getInventories() {
        return inventories;
    }

    public void setInventories(List<PciDeviceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryPciDeviceReply __example__() {
        APIQueryPciDeviceReply reply = new APIQueryPciDeviceReply();
        PciDeviceInventory inv = new PciDeviceInventory();

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}

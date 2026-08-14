package org.zstack.pciDevice;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by weiwang on 17/06/2017.
 */
@RestResponse(allTo = "inventories")
public class APIQueryPciDevicePciDeviceOfferingReply extends APIQueryReply {
    private List<PciDevicePciDeviceOfferingRefInventory> inventories;

    public List<PciDevicePciDeviceOfferingRefInventory>  getInventories() {
        return inventories;
    }

    public void setInventories(List<PciDevicePciDeviceOfferingRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryPciDevicePciDeviceOfferingReply __example__() {
        APIQueryPciDevicePciDeviceOfferingReply reply = new APIQueryPciDevicePciDeviceOfferingReply();
        PciDevicePciDeviceOfferingRefInventory inv = new PciDevicePciDeviceOfferingRefInventory();

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}

package org.zstack.softwarePackage.header;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQuerySoftwarePackageReply extends APIQueryReply {
    private List<SoftwarePackageInventory> inventories;

    public List<SoftwarePackageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SoftwarePackageInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySoftwarePackageReply __example__() {
        APIQuerySoftwarePackageReply reply = new APIQuerySoftwarePackageReply();
        reply.setInventories(list(SoftwarePackageInventory.__example__()));
        return reply;
    }
}

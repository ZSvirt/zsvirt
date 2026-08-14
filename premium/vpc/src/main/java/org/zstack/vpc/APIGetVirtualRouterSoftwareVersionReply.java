package org.zstack.vpc;


import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.service.virtualrouter.VirtualRouterSoftwareVersionInventory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * created by boce.wang 24/06/2022
 */
@RestResponse(allTo = "inventories")
public class APIGetVirtualRouterSoftwareVersionReply extends APIReply {
    List<VirtualRouterSoftwareVersionInventory> inventories;

    public List<VirtualRouterSoftwareVersionInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VirtualRouterSoftwareVersionInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVirtualRouterSoftwareVersionReply __example__() {
        APIGetVirtualRouterSoftwareVersionReply reply = new APIGetVirtualRouterSoftwareVersionReply();
        VirtualRouterSoftwareVersionInventory inv = new VirtualRouterSoftwareVersionInventory();
        inv.setUuid(uuid());
        inv.setSoftwareName("IPsec");
        inv.setCurrentVersion("4.5.2");
        inv.setLatestVersion("5.9.4");
        reply.setInventories(asList(inv));
        return reply;
    }
}

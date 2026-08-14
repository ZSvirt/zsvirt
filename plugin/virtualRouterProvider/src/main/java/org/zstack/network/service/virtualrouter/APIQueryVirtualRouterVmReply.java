package org.zstack.network.service.virtualrouter;

import org.zstack.header.query.APIQueryReply;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

/**
 */
public class APIQueryVirtualRouterVmReply extends APIQueryReply {
    private List<VirtualRouterVmInventory> inventories;

    public List<VirtualRouterVmInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VirtualRouterVmInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryVirtualRouterVmReply __example__() {
        APIQueryVirtualRouterVmReply reply = new APIQueryVirtualRouterVmReply();
        VirtualRouterVmInventory vr = new VirtualRouterVmInventory();

        vr.setName("Test-Router");
        vr.setDescription("this is a virtual router vm");
        vr.setClusterUuid(uuid());
        vr.setImageUuid(uuid());
        vr.setInstanceOfferingUuid(uuid());
        vr.setManagementNetworkUuid(uuid());
        vr.setPublicNetworkUuid(uuid());

        reply.setInventories(Arrays.asList(vr));
        return reply;
    }

}

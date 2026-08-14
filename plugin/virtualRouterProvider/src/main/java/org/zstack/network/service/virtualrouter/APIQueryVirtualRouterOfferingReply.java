package org.zstack.network.service.virtualrouter;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryVirtualRouterOfferingReply extends APIQueryReply {
    private List<VirtualRouterOfferingInventory> inventories;

    public List<VirtualRouterOfferingInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VirtualRouterOfferingInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryVirtualRouterOfferingReply __example__() {
        APIQueryVirtualRouterOfferingReply reply = new APIQueryVirtualRouterOfferingReply();
        VirtualRouterOfferingInventory vro = new VirtualRouterOfferingInventory();

        vro.setName("VirtualRouter-Offering");
        vro.setType(VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE);
        vro.setCpuNum(2);
        vro.setCpuSpeed(1);
        vro.setMemorySize(1024);
        vro.setZoneUuid(uuid());
        vro.setManagementNetworkUuid(uuid());
        vro.setImageUuid(uuid());
        vro.setPublicNetworkUuid(uuid());
        vro.setDefault(true);

        reply.setInventories(Arrays.asList(vro));
        return reply;
    }

}

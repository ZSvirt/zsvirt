package org.zstack.header.protocol;


import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIGetVpcAttachedOspfReply extends APIReply {
    private List<NetworkRouterAreaRefInventory> inventories;

    public List<NetworkRouterAreaRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NetworkRouterAreaRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVpcAttachedOspfReply __example__() {
        APIGetVpcAttachedOspfReply reply = new APIGetVpcAttachedOspfReply();

        NetworkRouterAreaRefInventory inv = new NetworkRouterAreaRefInventory();
        inv.setUuid(uuid());
        inv.setApplianceVmType("vpcvrouter");
        inv.setvRouterUuid(uuid());
        inv.setRouterAreaUuid(uuid());
        inv.setL3NetworkUuid(uuid());

        reply.setInventories(Arrays.asList(inv));


        return reply;
    }
}


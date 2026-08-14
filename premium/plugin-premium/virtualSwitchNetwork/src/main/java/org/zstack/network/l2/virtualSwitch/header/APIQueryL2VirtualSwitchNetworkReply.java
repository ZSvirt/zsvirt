package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryL2VirtualSwitchNetworkReply extends APIQueryReply {
    private List<L2VirtualSwitchNetworkInventory> inventories;

    public List<L2VirtualSwitchNetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2VirtualSwitchNetworkInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryL2VirtualSwitchNetworkReply __example__() {
        APIQueryL2VirtualSwitchNetworkReply reply = new APIQueryL2VirtualSwitchNetworkReply();
        L2VirtualSwitchNetworkInventory net = new L2VirtualSwitchNetworkInventory();

        net.setName("Test-dvs");
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setPhysicalInterface("eth0");
        net.setType(VirtualSwitchConstant.VIRTUAL_SWITCH_NETWORK_TYPE);

        reply.setInventories(Arrays.asList(net));
        return reply;
    }

}

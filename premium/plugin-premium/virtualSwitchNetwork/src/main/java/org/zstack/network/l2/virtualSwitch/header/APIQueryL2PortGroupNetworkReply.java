package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryL2PortGroupNetworkReply extends APIQueryReply {
    private List<L2PortGroupNetworkInventory> inventories;

    public List<L2PortGroupNetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2PortGroupNetworkInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryL2PortGroupNetworkReply __example__() {
        APIQueryL2PortGroupNetworkReply reply = new APIQueryL2PortGroupNetworkReply();
        L2PortGroupNetworkInventory net = new L2PortGroupNetworkInventory();

        net.setName("Test-pg");
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setPhysicalInterface("eth0");
        net.setVlanId(100);
        net.setType(VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE);

        reply.setInventories(Arrays.asList(net));
        return reply;
    }

}

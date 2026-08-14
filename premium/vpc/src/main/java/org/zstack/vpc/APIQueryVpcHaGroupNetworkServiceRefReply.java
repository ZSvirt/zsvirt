package org.zstack.vpc;

import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vpc.ha.VpcHaGroupNetworkServiceRefInventory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryVpcHaGroupNetworkServiceRefReply extends APIQueryReply {
    private List<VpcHaGroupNetworkServiceRefInventory> inventories;

    public List<VpcHaGroupNetworkServiceRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VpcHaGroupNetworkServiceRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVpcHaGroupNetworkServiceRefReply __example__() {
        APIQueryVpcHaGroupNetworkServiceRefReply reply = new APIQueryVpcHaGroupNetworkServiceRefReply();

        VpcHaGroupNetworkServiceRefInventory vpcHaNSRef = new VpcHaGroupNetworkServiceRefInventory();
        vpcHaNSRef.setId(1L);
        vpcHaNSRef.setNetworkServiceName(NetworkServiceType.SNAT.toString());
        vpcHaNSRef.setNetworkServiceUuid(uuid());
        vpcHaNSRef.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vpcHaNSRef.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(asList(vpcHaNSRef));

        return reply;
    }
}

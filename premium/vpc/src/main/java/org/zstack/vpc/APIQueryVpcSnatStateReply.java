package org.zstack.vpc;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vpc.VpcSnatStateInventory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryVpcSnatStateReply extends APIQueryReply {
    private List<VpcSnatStateInventory> inventories;

    public List<VpcSnatStateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VpcSnatStateInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVpcSnatStateReply __example__() {
        APIQueryVpcSnatStateReply reply = new APIQueryVpcSnatStateReply();

        VpcSnatStateInventory vpcSnatState = new VpcSnatStateInventory();
        vpcSnatState.setUuid(uuid());
        vpcSnatState.setVpcUuid(uuid());
        vpcSnatState.setL3NetworkUuid(uuid());
        vpcSnatState.setState(VpcStateEvent.enable.toString());
        vpcSnatState.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vpcSnatState.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(asList(vpcSnatState));

        return reply;
    }
}

package org.zstack.header.protocol;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryVRouterOspfNetworkReply extends APIQueryReply {
    private List<NetworkRouterAreaRefInventory> inventories;

    public List<NetworkRouterAreaRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NetworkRouterAreaRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVRouterOspfNetworkReply __example__() {
        APIQueryVRouterOspfNetworkReply reply = new APIQueryVRouterOspfNetworkReply();
        NetworkRouterAreaRefInventory ref = new NetworkRouterAreaRefInventory();
        ref.setUuid(uuid());
        ref.setRouterAreaUuid(uuid());
        ref.setvRouterUuid(uuid());
        ref.setL3NetworkUuid(uuid());
        ref.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ref.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setSuccess(true);
        reply.setInventories(asList(ref));

        return reply;
    }
}

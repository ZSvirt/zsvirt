package org.zstack.vpc;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vpc.VpcConnectionTO;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by weiwang on 20/11/2017
 */
@RestResponse(allTo = "inventories")
public class APIGetVpcVRouterDistributedRoutingConnectionsReply extends APIReply {
    private Map<String, Object> inventories;

    public Map<String, Object> getInventories() {
        return inventories;
    }

    public void setInventories(Map<String, Object> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVpcVRouterDistributedRoutingConnectionsReply __example__() {
        APIGetVpcVRouterDistributedRoutingConnectionsReply reply = new APIGetVpcVRouterDistributedRoutingConnectionsReply();

        VpcConnectionTO e = new VpcConnectionTO();
        e.setSourceL2NetworkType("L2VlanNetwork");
        e.setDestinationL2NetworkType("L2VlanNetwork");
        e.setDestinationL2NetworkVni(3105);
        e.setSourceL2NetworkVni(3101);
        e.setDestinationMac("fa:3a:b3:ae:f4:00");
        e.setSourceMac("fa:bf:6e:37:c3:00");
        e.setLastOpDate("2017-12-02 15:23:44.872099448 +0800 CST m=+70467.712498312");
        e.setStatus("ZSNP_DST_SUCC");

        HashMap<String, Object> map = new HashMap<>();
        map.put("192.168.31.156,192.168.105.175", e);
        reply.setInventories(map);
        return reply;
    }
}

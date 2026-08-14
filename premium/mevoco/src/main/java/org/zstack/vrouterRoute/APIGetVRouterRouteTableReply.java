package org.zstack.vrouterRoute;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.*;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestResponse(allTo = "inventories")
public class APIGetVRouterRouteTableReply extends APIReply {
    private List<VRouterRouteEntryAO> inventories = new ArrayList<>();

    public List<VRouterRouteEntryAO> getInventories() {
        return inventories;
    }

    public void setInventories(List<VRouterRouteEntryAO> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVRouterRouteTableReply __example__() {
        APIGetVRouterRouteTableReply reply = new APIGetVRouterRouteTableReply();
        List<VRouterRouteEntryAO> invs = new ArrayList<>();
        VRouterRouteEntryAO ao1 = new VRouterRouteEntryAO();
        ao1.setDestination("0.0.0.0/0");
        ao1.setTarget("100.64.201.1");
        ao1.setStatus("selected");
        ao1.setType(VRouterRouteEntryType.ZStack);

        VRouterRouteEntryAO ao2 = new VRouterRouteEntryAO();
        ao2.setDestination("100.64.0.0/24");
        ao2.setTarget("100.64.201.10");
        ao2.setStatus("selected");
        ao2.setType(VRouterRouteEntryType.UserStatic);
        ao2.setDistance(128);
        ao2.setUuid(uuid());

        VRouterRouteEntryAO ao3 = new VRouterRouteEntryAO();
        ao3.setDestination("100.64.0.0/24");
        ao3.setTarget("100.64.201.11");
        ao3.setStatus("active");
        ao3.setType(VRouterRouteEntryType.UserStatic);
        ao3.setDistance(128);
        ao3.setUuid(uuid());

        VRouterRouteEntryAO ao4 = new VRouterRouteEntryAO();
        ao4.setDestination("10.0.57.94/32");
        ao4.setTarget("eth0");
        ao4.setType(VRouterRouteEntryType.ZStack);

        VRouterRouteEntryAO ao5 = new VRouterRouteEntryAO();
        ao5.setDestination("10.0.57.94/32");
        ao5.setTarget("100.64.201.12");
        ao5.setStatus("inactive");
        ao5.setType(VRouterRouteEntryType.UserStatic);
        ao5.setDistance(1);
        ao5.setUuid(uuid());

        VRouterRouteEntryAO ao6 = new VRouterRouteEntryAO();
        ao6.setDestination("192.168.197.0/24");
        ao6.setTarget("eth1");
        ao6.setStatus("selected");
        ao6.setType(VRouterRouteEntryType.DirectConnect);

        VRouterRouteEntryAO ao7 = new VRouterRouteEntryAO();
        ao7.setDestination("192.168.197.0/24");
        ao7.setTarget("100.64.201.13");
        ao7.setStatus("inactive");
        ao7.setType(VRouterRouteEntryType.DirectConnect);
        ao7.setDistance(1);
        ao7.setUuid(uuid());

        VRouterRouteEntryAO ao8 = new VRouterRouteEntryAO();
        ao8.setDestination("192.168.198.0/24");
        ao8.setStatus("selected");
        ao8.setType(VRouterRouteEntryType.UserBlackHole);
        ao8.setDistance(1);
        ao8.setUuid(uuid());

        invs.addAll(Arrays.asList(ao1, ao2, ao3, ao4, ao5, ao6, ao7, ao8));
        reply.setInventories(invs);
        reply.setSuccess(true);
        return reply;
    }
}

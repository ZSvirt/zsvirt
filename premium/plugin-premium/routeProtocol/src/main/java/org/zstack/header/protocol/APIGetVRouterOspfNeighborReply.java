package org.zstack.header.protocol;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

@RestResponse(allTo = "neighbors")
public class APIGetVRouterOspfNeighborReply  extends APIReply {
    private List<Neighbor> neighbors;

    public List<Neighbor> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(List<Neighbor> neighbors) {
        this.neighbors = neighbors;
    }

    public static APIGetVRouterOspfNeighborReply __example__() {
        APIGetVRouterOspfNeighborReply reply = new APIGetVRouterOspfNeighborReply();
        List<Neighbor> ns = new ArrayList<>();

        Neighbor n1 = new Neighbor();
        n1.setId(uuid());
        n1.setPriority("1");
        n1.setState("Full/DR");
        n1.setDeadTime("30s");
        n1.setNeighborAddress("192.168.48.1");
        n1.setDevice("eth2:192.168.48.5");

        Neighbor n2 = new Neighbor();
        n2.setId(uuid());
        n2.setPriority("1");
        n2.setState("Full/DR");
        n2.setDeadTime("30s");
        n2.setNeighborAddress("192.168.48.1");
        n2.setDevice("eth2:192.168.48.5");

        ns.add(n1);
        ns.add(n2);
        reply.setNeighbors(ns);
        reply.setSuccess(true);
        return reply;
    }
}

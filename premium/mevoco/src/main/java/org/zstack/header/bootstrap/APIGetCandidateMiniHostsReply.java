package org.zstack.header.bootstrap;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(fieldsTo = {"hosts"})
public class APIGetCandidateMiniHostsReply extends APIReply {
    private List<MiniCandidateHostStruct> hosts;

    public List<MiniCandidateHostStruct> getHosts() {
        return hosts;
    }

    public void setHosts(List<MiniCandidateHostStruct> hosts) {
        this.hosts = hosts;
    }

    public static APIGetCandidateMiniHostsReply __example__() {
        APIGetCandidateMiniHostsReply reply = new APIGetCandidateMiniHostsReply();

        MiniCandidateHostStruct h1 = new MiniCandidateHostStruct();
        h1.setSn("RD-BJD-001-07311A");
        h1.setHostName("zstack-mini-07311a");
        h1.setIpv4Address("172.20.26.21");
        h1.setIpv4Interface("br_bond0_26");
        h1.setIpv6Address("fe80::ae1f:6bff:fe93:9a72");
        h1.setIpv6Interface("br_bond0_26");
        h1.setManufacturer("ZStack");
        h1.setProduct("ZOHCN05");

        MiniCandidateHostStruct h2 = new MiniCandidateHostStruct();
        h2.setSn("RD-BJD-001-07311B");
        h2.setHostName("zstack-mini-07311b");
        h2.setIpv4Address("172.20.26.22");
        h2.setIpv4Interface("br_bond0_26");
        h2.setIpv6Address("fe80::ae1f:6bff:fe92:8a7a");
        h2.setIpv6Interface("br_bond0_26");
        h2.setManufacturer("ZStack");
        h2.setProduct("ZOHCN05");
        reply.setHosts(Arrays.asList(h1, h2));
        return reply;
    }
}

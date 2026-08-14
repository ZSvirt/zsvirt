package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/cluster/hosts-network-interfaces",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateNetworkInterfacesReply.class
)
public class APIGetCandidateNetworkInterfacesMsg extends APIGetMessage {
    /**
     * @desc uuids of host which is going to create bonding or update service type
     */
    @APIParam(resourceType = HostVO.class)
    private List<String> hostUuids;

    @APIParam(validValues = {"interface", "bonding", "all"}, required = false)
    private String interfaceType = "all";

    @APIParam(required = false)
    private boolean intersecting = true;

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public boolean isIntersecting() {
        return intersecting;
    }

    public void setIntersecting(boolean intersecting) {
        this.intersecting = intersecting;
    }

    public static APIGetCandidateNetworkInterfacesMsg __example__() {
        APIGetCandidateNetworkInterfacesMsg msg = new APIGetCandidateNetworkInterfacesMsg();
        msg.setHostUuids(Collections.singletonList(uuid()));
        return msg;
    }
}

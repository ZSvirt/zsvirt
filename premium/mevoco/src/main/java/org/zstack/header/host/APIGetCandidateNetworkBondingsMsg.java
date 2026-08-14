package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/cluster/hosts-network-bondings",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateNetworkBondingsReply.class
)
public class APIGetCandidateNetworkBondingsMsg extends APIGetMessage {
    /**
     * @desc uuids of host which is going to get bonding
     */
    @APIParam(resourceType = HostVO.class)
    private List<String> hostUuids;

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    public static APIGetCandidateNetworkBondingsMsg __example__() {
        APIGetCandidateNetworkBondingsMsg msg = new APIGetCandidateNetworkBondingsMsg();
        msg.setHostUuids(Collections.singletonList(uuid()));
        return msg;
    }
}

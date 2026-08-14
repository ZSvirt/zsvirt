package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @author: sulin.sheng
 * @date: 2021-11-24
 **/
@RestRequest(
        path = "/load-balancers/servergroups/candidate-l3network",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateL3NetworksForServerGroupReply.class
)
public class APIGetCandidateL3NetworksForServerGroupMsg extends APIGetMessage implements LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerServerGroupVO.class, required = false)
    private String serverGroupUuid;
    @APIParam(resourceType = LoadBalancerVO.class, required = false)
    private String loadBalancerUuid;

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public static APIGetCandidateL3NetworksForServerGroupMsg __example__() {
        APIGetCandidateL3NetworksForServerGroupMsg msg = new APIGetCandidateL3NetworksForServerGroupMsg();
        msg.setLoadBalancerUuid(uuid());

        return msg;
    }
}

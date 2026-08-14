package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by shixin.ruan on 2020/11/09.
 */
@RestRequest(
        path = "/load-balancers/servergroups/candidate-nics",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateVmNicsForLoadBalancerServerGroupReply.class
)
public class APIGetCandidateVmNicsForLoadBalancerServerGroupMsg extends APISyncCallMessage implements LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerServerGroupVO.class, required = false)
    private String servergroupUuid;
    @APIParam(resourceType = LoadBalancerVO.class, required = false)
    private String loadBalancerUuid;

    public String getServergroupUuid() {
        return servergroupUuid;
    }

    public void setServergroupUuid(String servergroupUuid) {
        this.servergroupUuid = servergroupUuid;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
 
    public static APIGetCandidateVmNicsForLoadBalancerServerGroupMsg __example__() {
        APIGetCandidateVmNicsForLoadBalancerServerGroupMsg msg = new APIGetCandidateVmNicsForLoadBalancerServerGroupMsg();

        msg.setServergroupUuid(uuid());
        msg.setLoadBalancerUuid(uuid());

        return msg;
    }

}

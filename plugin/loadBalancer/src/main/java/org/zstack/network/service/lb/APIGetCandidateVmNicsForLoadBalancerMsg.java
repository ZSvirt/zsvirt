package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/11/29.
 */
@RestRequest(
        path = "/load-balancers/listeners/{listenerUuid}/vm-instances/candidate-nics",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateVmNicsForLoadBalancerReply.class
)
public class APIGetCandidateVmNicsForLoadBalancerMsg extends APISyncCallMessage implements LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerListenerVO.class)
    private String listenerUuid;
    @APINoSee
    private String loadBalancerUuid;

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
 
    public static APIGetCandidateVmNicsForLoadBalancerMsg __example__() {
        APIGetCandidateVmNicsForLoadBalancerMsg msg = new APIGetCandidateVmNicsForLoadBalancerMsg();

        msg.setListenerUuid(uuid());
        msg.setLoadBalancerUuid(uuid());

        return msg;
    }

}

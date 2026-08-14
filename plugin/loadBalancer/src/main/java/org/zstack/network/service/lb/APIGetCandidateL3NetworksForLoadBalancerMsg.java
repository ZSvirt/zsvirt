package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * @author: zhanyong.miao
 * @date: 2020-04-15
 **/
@RestRequest(
        path = "/load-balancers/listeners/{listenerUuid}/networks/candidates",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateL3NetworksForLoadBalancerReply.class
)
public class APIGetCandidateL3NetworksForLoadBalancerMsg extends APIGetMessage implements LoadBalancerMessage {
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


    public static APIGetCandidateL3NetworksForLoadBalancerMsg __example__() {
        APIGetCandidateL3NetworksForLoadBalancerMsg msg = new APIGetCandidateL3NetworksForLoadBalancerMsg();

        msg.setListenerUuid(uuid());
        msg.setLoadBalancerUuid(uuid());

        return msg;
    }
}

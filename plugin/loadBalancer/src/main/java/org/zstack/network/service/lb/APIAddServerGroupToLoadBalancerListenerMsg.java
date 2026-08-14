package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/load-balancers/listeners/{listenerUuid}/servergroups",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddServerGroupToLoadBalancerListenerEvent.class
)
public class APIAddServerGroupToLoadBalancerListenerMsg extends APIMessage implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = LoadBalancerServerGroupVO.class, nonempty = true)
    private String serverGroupUuid;
    @APIParam(resourceType = LoadBalancerListenerVO.class)
    private String listenerUuid;
    @APINoSee
    private String loadBalancerUuid;

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuids(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }

    public String getlistenerUuid() {
        return listenerUuid;
    }

    public void setlistenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public static APIAddServerGroupToLoadBalancerListenerMsg __example__() {
        APIAddServerGroupToLoadBalancerListenerMsg msg = new APIAddServerGroupToLoadBalancerListenerMsg();
        msg.setlistenerUuid(uuid());
        msg.setLoadBalancerUuid(uuid());
        msg.setServerGroupUuids(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIAddServerGroupToLoadBalancerListenerMsg)msg).getLoadBalancerUuid(), LoadBalancerVO.class);
    }

}

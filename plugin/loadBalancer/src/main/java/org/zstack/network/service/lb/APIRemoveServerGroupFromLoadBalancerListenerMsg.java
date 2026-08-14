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
        method = HttpMethod.DELETE,
        responseClass = APIRemoveServerGroupFromLoadBalancerListenerEvent.class
)
public class APIRemoveServerGroupFromLoadBalancerListenerMsg extends APIMessage implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = LoadBalancerServerGroupVO.class, nonempty = true)
    private String serverGroupUuid;
    @APIParam(resourceType = LoadBalancerListenerVO.class)
    private String listenerUuid;
    @APINoSee
    private String loadBalancerUuid;

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setlistenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public static APIRemoveServerGroupFromLoadBalancerListenerMsg __example__() {
        APIRemoveServerGroupFromLoadBalancerListenerMsg msg = new APIRemoveServerGroupFromLoadBalancerListenerMsg();

        msg.setlistenerUuid(uuid());
        msg.setServerGroupUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIRemoveServerGroupFromLoadBalancerListenerMsg)msg).loadBalancerUuid, LoadBalancerVO.class);
    }
}

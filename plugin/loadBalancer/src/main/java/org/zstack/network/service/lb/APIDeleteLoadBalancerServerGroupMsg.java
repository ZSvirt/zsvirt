package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/load-balancers/servergroups/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteLoadBalancerServerGroupEvent.class
)
public class APIDeleteLoadBalancerServerGroupMsg extends APIMessage implements LoadBalancerMessage, APIAuditor{
    @APIParam(resourceType = LoadBalancerServerGroupVO.class, successIfResourceNotExisting = true)
    private String uuid;

    @APINoSee
    private String loadBalancerUuid;


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public static APIDeleteLoadBalancerServerGroupMsg __example__() {
        APIDeleteLoadBalancerServerGroupMsg msg = new APIDeleteLoadBalancerServerGroupMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIDeleteLoadBalancerServerGroupMsg)msg).getUuid(), LoadBalancerVO.class);
    }



}

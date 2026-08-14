package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 8/8/2015.
 */
@RestRequest(
        path = "/load-balancers/listeners/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteLoadBalancerListenerEvent.class
)
public class APIDeleteLoadBalancerListenerMsg extends APIMessage implements LoadBalancerMessage, APIAuditor {
    @APINoSee
    private String loadBalancerUuid;
    @APIParam(resourceType = LoadBalancerListenerVO.class, successIfResourceNotExisting = true)
    private String uuid;

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIDeleteLoadBalancerListenerMsg __example__() {
        APIDeleteLoadBalancerListenerMsg msg = new APIDeleteLoadBalancerListenerMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIDeleteLoadBalancerListenerMsg)msg).getUuid(), LoadBalancerVO.class);
    }
}

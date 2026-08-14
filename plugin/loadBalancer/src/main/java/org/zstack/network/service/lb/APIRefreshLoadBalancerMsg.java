package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 8/18/2015.
 */
@RestRequest(
        path = "/load-balancers/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIRefreshLoadBalancerEvent.class,
        isAction = true
)
public class APIRefreshLoadBalancerMsg extends APIMessage implements LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return uuid;
    }
 
    public static APIRefreshLoadBalancerMsg __example__() {
        APIRefreshLoadBalancerMsg msg = new APIRefreshLoadBalancerMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

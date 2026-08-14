package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/load-balancers/servergroups/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateLoadBalancerServerGroupEvent.class,
        isAction = true
)
public class APIUpdateLoadBalancerServerGroupMsg extends APIMessage implements LoadBalancerMessage{
    @APIParam(resourceType = LoadBalancerServerGroupVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APINoSee
    private String loadBalancerUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public static APIUpdateLoadBalancerServerGroupMsg __example__() {
        APIUpdateLoadBalancerServerGroupMsg msg = new APIUpdateLoadBalancerServerGroupMsg();
        msg.setUuid(uuid());
        msg.setName("updateServerGroup");
        msg.setDescription("updateServerGroup");
        return msg;
    }

}

package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by camile on 5/19/2017.
 */
@RestRequest(
        path = "/load-balancers/listeners/{uuid}",
        method = HttpMethod.PUT,
        responseClass = APIUpdateLoadBalancerListenerEvent.class,
        isAction = true
)
public class APIUpdateLoadBalancerListenerMsg extends APIMessage implements LoadBalancerListenerMsg , LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerListenerVO.class)
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

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    @Override
    public String getLoadBalancerListenerUuid() {
        return uuid;
    }

    public static APIUpdateLoadBalancerListenerMsg __example__() {
        APIUpdateLoadBalancerListenerMsg msg = new APIUpdateLoadBalancerListenerMsg();

        msg.setUuid(uuid());
        msg.setName("Test-Listener");
        msg.setDescription("desc info");

        return msg;
    }
}

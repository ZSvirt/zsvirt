package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by camile on 5/18/2017.
 */
@RestRequest(
        path = "/load-balancers/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateLoadBalancerEvent.class,
        isAction = true
        //parameterName = "updateLoadBalancer"
)
public class APIUpdateLoadBalancerMsg extends APICreateMessage  implements LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getLoadBalancerUuid(){
        return uuid;
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

    public static APIUpdateLoadBalancerMsg __example__() {
        APIUpdateLoadBalancerMsg msg = new APIUpdateLoadBalancerMsg();
        msg.uuid = uuid();
        msg.setDescription("info");
        msg.setName("Test-Lb");

        return msg;
    }
}

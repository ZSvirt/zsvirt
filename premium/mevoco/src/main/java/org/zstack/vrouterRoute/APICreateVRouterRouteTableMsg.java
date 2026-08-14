package org.zstack.vrouterRoute;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * Created by weiwang on 15/06/2017.
 */
@TagResourceType(VRouterRouteTableVO.class)
@RestRequest(
        path = "/vrouter-route-tables",
        method = HttpMethod.POST,
        responseClass = APICreateVRouterRouteTableEvent.class,
        parameterName = "params"
)
public class APICreateVRouterRouteTableMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

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

    public static APICreateVRouterRouteTableMsg __example__() {
        APICreateVRouterRouteTableMsg msg = new APICreateVRouterRouteTableMsg();
        msg.setName("Test-VRouterRouteTable");
        msg.setDescription("Test-VRouterRouteTable");

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateVRouterRouteTableEvent)rsp).getInventory().getUuid() : "", VRouterRouteTableVO.class);
    }
}

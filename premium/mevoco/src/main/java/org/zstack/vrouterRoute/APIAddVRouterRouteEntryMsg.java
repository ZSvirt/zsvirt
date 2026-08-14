package org.zstack.vrouterRoute;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;


/**
 * Created by weiwang on 15/06/2017.
 */
@RestRequest(
        path = "/vrouter-route-tables/{routeTableUuid}/route-entries",
        method = HttpMethod.POST,
        responseClass = APIAddVRouterRouteEntryEvent.class,
        parameterName = "params"
)
public class APIAddVRouterRouteEntryMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(maxLength = 32, required = false)
    private String type;

    @APIParam(resourceType = VRouterRouteTableVO.class)
    private String routeTableUuid;

    @APIParam(maxLength = 64)
    private String destination;

    @APIParam(maxLength = 64, required = false)
    private String target;

    @APIParam(required = false, numberRange = {1,254})
    private Integer distance;

    public static APIAddVRouterRouteEntryMsg __example__() {
        APIAddVRouterRouteEntryMsg msg = new APIAddVRouterRouteEntryMsg();
        msg.setDescription("Test route");
        msg.setType(VRouterRouteEntryType.UserStatic.toString());
        msg.setRouteTableUuid(uuid());
        msg.setDestination("192.168.2.0/24");
        msg.setTarget("172.20.1.1");
        return msg;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRouteTableUuid() {
        return routeTableUuid;
    }

    public void setRouteTableUuid(String routeTableUuid) {
        this.routeTableUuid = routeTableUuid;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddVRouterRouteEntryEvent)rsp).getInventory().getUuid() : "", VRouterRouteEntryVO.class);
    }
}

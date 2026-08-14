package org.zstack.vrouterRoute;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestRequest(
        path = "/vrouter-route-tables/{routeTableUuid}/route-entries/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVRouterRouteEntryEvent.class
)
public class APIDeleteVRouterRouteEntryMsg extends APIDeleteMessage {
    @APIParam(resourceType = VRouterRouteEntryVO.class)
    private String uuid;

    @APIParam(resourceType = VRouterRouteTableVO.class)
    private String routeTableUuid;

    public static APIDeleteVRouterRouteEntryMsg __example__() {
        APIDeleteVRouterRouteEntryMsg msg = new APIDeleteVRouterRouteEntryMsg();
        msg.setRouteTableUuid(uuid());
        msg.setUuid(uuid());
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getRouteTableUuid() {
        return routeTableUuid;
    }

    public void setRouteTableUuid(String routeTableUuid) {
        this.routeTableUuid = routeTableUuid;
    }
}

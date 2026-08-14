package org.zstack.vrouterRoute;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestRequest(
        path = "/vrouter-route-tables/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVRouterRouteTableEvent.class
)
public class APIDeleteVRouterRouteTableMsg extends APIDeleteMessage {
    @APIParam(resourceType = VRouterRouteTableVO.class)
    private String uuid;

    public static APIDeleteVRouterRouteTableMsg __example__() {
        APIDeleteVRouterRouteTableMsg msg = new APIDeleteVRouterRouteTableMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

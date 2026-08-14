package org.zstack.vrouterRoute;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;


/**
 * Created by fang.sun on 1/24/2018.
 */
@RestRequest(
        path = "/vrouter-route-tables/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateVRouterRouteTableEvent.class,
        isAction = true
)
public class APIUpdateVRouterRouteTableMsg extends APIMessage {
    @APIParam(resourceType = VRouterRouteTableVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false, emptyString = false)
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIUpdateVRouterRouteTableMsg __example__() {
        APIUpdateVRouterRouteTableMsg msg = new APIUpdateVRouterRouteTableMsg();
        msg.setUuid(uuid());
        msg.setDescription("test description");
        msg.setName("Test-vrrtahle");
        return msg;
    }

}

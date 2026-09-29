package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.APIUpdateL3NetworkMsg;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/l3-networks/port-group/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdatePortGroupEvent.class
)
public class APIUpdatePortGroupMsg extends APIUpdateL3NetworkMsg {
    @APIParam(required = false, maxLength = 32, emptyString = false)
    private String tenantRouterUuid;

    public String getTenantRouterUuid() {
        return tenantRouterUuid;
    }

    public void setTenantRouterUuid(String tenantRouterUuid) {
        this.tenantRouterUuid = tenantRouterUuid;
    }
}

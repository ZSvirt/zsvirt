package org.zstack.header.baremetal.pxeserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2018-10-11.
 */
@RestRequest(
        path = "/baremetal/pxeservers/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIReconnectBaremetalPxeServerEvent.class
)
public class APIReconnectBaremetalPxeServerMsg extends APIMessage {
    @APIParam(resourceType = BaremetalPxeServerVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIReconnectBaremetalPxeServerMsg __example__() {
        APIReconnectBaremetalPxeServerMsg msg = new APIReconnectBaremetalPxeServerMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

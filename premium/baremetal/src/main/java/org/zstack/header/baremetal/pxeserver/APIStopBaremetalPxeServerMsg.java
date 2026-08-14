package org.zstack.header.baremetal.pxeserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 5/9/17.
 */
@RestRequest(
        path = "/baremetal/pxeservers/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIStopBaremetalPxeServerEvent.class
)
public class APIStopBaremetalPxeServerMsg extends APIMessage {
    @APIParam(resourceType = BaremetalPxeServerVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIStopBaremetalPxeServerMsg __example__() {
        APIStopBaremetalPxeServerMsg msg = new APIStopBaremetalPxeServerMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

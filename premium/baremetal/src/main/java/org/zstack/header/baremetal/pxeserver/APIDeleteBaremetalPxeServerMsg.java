package org.zstack.header.baremetal.pxeserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2017/5/7.
 */
@RestRequest(
        path = "/baremetal/pxeservers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteBaremetalPxeServerEvent.class
)
public class APIDeleteBaremetalPxeServerMsg extends APIDeleteMessage {
    @APIParam(resourceType = BaremetalPxeServerVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteBaremetalPxeServerMsg __example__() {
        APIDeleteBaremetalPxeServerMsg msg = new APIDeleteBaremetalPxeServerMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

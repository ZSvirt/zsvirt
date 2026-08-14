package org.zstack.header.baremetal.instance;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 7/4/18.
 */
@RestRequest(
        path = "/baremetal/instances/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDestroyBaremetalInstanceEvent.class
)
public class APIDestroyBaremetalInstanceMsg extends APIDeleteMessage implements BaremetalInstanceMessage {
    @APIParam(resourceType = BaremetalInstanceVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getBaremetalInstanceUuid() {
        return getUuid();
    }

    public static APIDestroyBaremetalInstanceMsg __example__() {
        APIDestroyBaremetalInstanceMsg msg = new APIDestroyBaremetalInstanceMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

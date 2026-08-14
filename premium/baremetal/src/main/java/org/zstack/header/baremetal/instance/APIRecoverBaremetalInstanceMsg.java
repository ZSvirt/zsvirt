package org.zstack.header.baremetal.instance;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 7/6/18.
 */
@RestRequest(
        path = "/baremetal/instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIRecoverBaremetalInstanceEvent.class
)
public class APIRecoverBaremetalInstanceMsg extends APIMessage implements BaremetalInstanceMessage {
    @APIParam(resourceType = BaremetalInstanceVO.class)
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

    public static APIRecoverBaremetalInstanceMsg __example__() {
        APIRecoverBaremetalInstanceMsg msg = new APIRecoverBaremetalInstanceMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

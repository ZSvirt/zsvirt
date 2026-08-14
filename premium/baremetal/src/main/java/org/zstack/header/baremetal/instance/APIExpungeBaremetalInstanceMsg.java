package org.zstack.header.baremetal.instance;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 7/4/18.
 */
@RestRequest(
        path = "/baremetal/instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIExpungeBaremetalInstanceEvent.class
)
public class APIExpungeBaremetalInstanceMsg extends APIMessage implements BaremetalInstanceMessage {
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

    public static APIExpungeBaremetalInstanceMsg __example__() {
        APIExpungeBaremetalInstanceMsg msg = new APIExpungeBaremetalInstanceMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

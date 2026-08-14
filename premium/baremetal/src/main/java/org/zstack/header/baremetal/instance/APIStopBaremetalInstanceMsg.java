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
        responseClass = APIStopBaremetalInstanceEvent.class
)
public class APIStopBaremetalInstanceMsg extends APIMessage implements BaremetalInstanceMessage {
    @APIParam(resourceType = BaremetalInstanceVO.class)
    private String uuid;

    @APIParam(required = false, validValues = {"grace", "cold"})
    private String type = "grace";

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getBaremetalInstanceUuid() {
        return getUuid();
    }

    public static APIStopBaremetalInstanceMsg __example__() {
        APIStopBaremetalInstanceMsg msg = new APIStopBaremetalInstanceMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

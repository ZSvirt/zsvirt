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
        responseClass = APIStartBaremetalInstanceEvent.class
)
public class APIStartBaremetalInstanceMsg extends APIMessage implements BaremetalInstanceMessage {
    @APIParam(resourceType = BaremetalInstanceVO.class)
    private String uuid;

    @APIParam(required = false)
    private Boolean pxeBoot = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Boolean getPxeBoot() {
        return pxeBoot;
    }

    public void setPxeBoot(Boolean pxeBoot) {
        this.pxeBoot = pxeBoot;
    }

    @Override
    public String getBaremetalInstanceUuid() {
        return getUuid();
    }

    public static APIStartBaremetalInstanceMsg __example__() {
        APIStartBaremetalInstanceMsg msg = new APIStartBaremetalInstanceMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

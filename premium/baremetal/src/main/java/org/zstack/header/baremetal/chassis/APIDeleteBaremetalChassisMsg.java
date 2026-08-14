package org.zstack.header.baremetal.chassis;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 4/26/17.
 */
@RestRequest(
        path = "/baremetal/chassis/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteBaremetalChassisEvent.class
)
public class APIDeleteBaremetalChassisMsg extends APIDeleteMessage implements BaremetalChassisMessage {
    @APIParam(resourceType = BaremetalChassisVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getBaremetalChassisUuid() {
        return uuid;
    }

    public static APIDeleteBaremetalChassisMsg __example__() {
        APIDeleteBaremetalChassisMsg msg = new APIDeleteBaremetalChassisMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

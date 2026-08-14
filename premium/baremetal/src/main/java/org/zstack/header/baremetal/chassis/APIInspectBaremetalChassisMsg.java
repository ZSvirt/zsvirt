package org.zstack.header.baremetal.chassis;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 7/16/18.
 */
@RestRequest(
        path = "/baremetal/chassis/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIInspectBaremetalChassisEvent.class
)
public class APIInspectBaremetalChassisMsg extends APIMessage {
    @APIParam(resourceType = BaremetalChassisVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIInspectBaremetalChassisMsg __example__() {
        APIInspectBaremetalChassisMsg msg = new APIInspectBaremetalChassisMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

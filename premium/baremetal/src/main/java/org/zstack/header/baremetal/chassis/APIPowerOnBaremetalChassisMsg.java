package org.zstack.header.baremetal.chassis;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 7/16/18.
 */
@RestRequest(
        path = "/baremetal/chassis/{chassisUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIPowerOnBaremetalChassisEvent.class
)
public class APIPowerOnBaremetalChassisMsg extends APIMessage {
    @APIParam(resourceType = BaremetalChassisVO.class)
    private String chassisUuid;

    public String getChassisUuid() {
        return chassisUuid;
    }

    public void setChassisUuid(String chassisUuid) {
        this.chassisUuid = chassisUuid;
    }

    public static APIPowerOnBaremetalChassisMsg __example__() {
        APIPowerOnBaremetalChassisMsg msg = new APIPowerOnBaremetalChassisMsg();
        msg.setChassisUuid(uuid());
        return msg;
    }
}

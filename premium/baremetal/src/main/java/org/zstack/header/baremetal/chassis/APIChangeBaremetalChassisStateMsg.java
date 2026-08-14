package org.zstack.header.baremetal.chassis;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 7/6/18.
 */
@RestRequest(
        path = "/baremetal/chassis/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIChangeBaremetalChassisStateEvent.class
)
public class APIChangeBaremetalChassisStateMsg extends APIMessage implements BaremetalChassisMessage {
    @APIParam(resourceType = BaremetalChassisVO.class)
    private String uuid;

    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    @Override
    public String getBaremetalChassisUuid() {
        return getUuid();
    }

    public static APIChangeBaremetalChassisStateMsg __example__() {
        APIChangeBaremetalChassisStateMsg msg = new APIChangeBaremetalChassisStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent("enable");
        return msg;
    }
}

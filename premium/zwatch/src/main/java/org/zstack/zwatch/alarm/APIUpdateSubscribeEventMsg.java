package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.datatype.EmergencyLevel;

@RestRequest(
        path = "/zwatch/events/subscriptions/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateSubscribeEventEvent.class,
        isAction = true)
public class APIUpdateSubscribeEventMsg extends APIMessage {
    @APIParam(resourceType = EventSubscriptionVO.class)
    private String uuid;

    @APIParam(required = false, validValues = {"Emergent", "Important", "Normal"})
    private String emergencyLevel;

    @APIParam(maxLength = 255, required = false)
    private String name;

    public static APIUpdateSubscribeEventMsg __example__() {
        APIUpdateSubscribeEventMsg ret = new APIUpdateSubscribeEventMsg();
        ret.uuid = uuid(EventSubscriptionVO.class);
        ret.setEmergencyLevel(EmergencyLevel.Emergent.name());
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

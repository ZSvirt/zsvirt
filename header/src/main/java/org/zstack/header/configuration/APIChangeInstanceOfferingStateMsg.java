package org.zstack.header.configuration;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:02 PM
 * To change this template use File | Settings | File Templates.
 */
@RestRequest(
        path = "/instance-offerings/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIChangeInstanceOfferingStateEvent.class
)
public class APIChangeInstanceOfferingStateMsg extends APIMessage implements InstanceOfferingMessage {
    @APIParam(resourceType = InstanceOfferingVO.class)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    @Override
    public String getInstanceOfferingUuid() {
        return getUuid();
    }

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
 
    public static APIChangeInstanceOfferingStateMsg __example__() {
        APIChangeInstanceOfferingStateMsg msg = new APIChangeInstanceOfferingStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(InstanceOfferingStateEvent.enable.toString());
        return msg;
    }
}

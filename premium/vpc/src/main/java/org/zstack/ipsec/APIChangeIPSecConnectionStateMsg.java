package org.zstack.ipsec;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 */
@RestRequest(
        path = "/ipsec/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeIPSecConnectionStateEvent.class,
        isAction = true
)
public class APIChangeIPSecConnectionStateMsg extends APIMessage implements IPsecConnectionMessage {
    @APIParam(resourceType = IPsecConnectionVO.class)
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
    public String getIPsecConnectionUuid() {
        return uuid;
    }

    public static APIChangeIPSecConnectionStateMsg __example__() {
        APIChangeIPSecConnectionStateMsg msg = new APIChangeIPSecConnectionStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(IPSecStateEvent.enable.toString());

        return msg;
    }
}

package org.zstack.network.service.vip;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 */
@RestRequest(
        path = "/vips/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeVipStateEvent.class,
        isAction = true
)
public class APIChangeVipStateMsg extends APIMessage implements VipMessage {
    @APIParam(resourceType = VipVO.class)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String vipUuid) {
        this.uuid = vipUuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    @Override
    public String getVipUuid() {
        return uuid;
    }
 
    public static APIChangeVipStateMsg __example__() {
        APIChangeVipStateMsg msg = new APIChangeVipStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(VipStateEvent.enable.toString());

        return msg;
    }
}

package org.zstack.header.affinitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 */
@RestRequest(
        path = "/affinity-groups/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeAffinityGroupStateEvent.class,
        isAction = true
)
public class APIChangeAffinityGroupStateMsg extends APIMessage implements AffinityGroupMessage {
    @APIParam(resourceType = AffinityGroupVO.class)
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
    public String getAffinityGroupUuid() {
        return uuid;
    }

    public static APIChangeAffinityGroupStateMsg __example__() {
        APIChangeAffinityGroupStateMsg msg = new APIChangeAffinityGroupStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(AffinityGroupStateEvent.enable.toString());

        return msg;
    }
}

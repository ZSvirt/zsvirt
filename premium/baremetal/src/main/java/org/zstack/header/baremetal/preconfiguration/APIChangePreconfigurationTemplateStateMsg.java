package org.zstack.header.baremetal.preconfiguration;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 2018-12-26.
 */
@RestRequest(
        path = "/baremetal/preconfigurations/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangePreconfigurationTemplateStateEvent.class,
        isAction = true
)
public class APIChangePreconfigurationTemplateStateMsg extends APIMessage implements PreconfigurationTemplateMessage {
    @APIParam(resourceType = PreconfigurationTemplateVO.class)
    private String uuid;

    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public PreconfigurationTemplateStateEvent getStateEvent() {
        return PreconfigurationTemplateStateEvent.valueOf(stateEvent);
    }

    public void setStateEvent(PreconfigurationTemplateStateEvent stateEvent) {
        this.stateEvent = stateEvent.toString();
    }

    public static APIChangePreconfigurationTemplateStateMsg __example__() {
        APIChangePreconfigurationTemplateStateMsg msg = new APIChangePreconfigurationTemplateStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(PreconfigurationTemplateStateEvent.disable);
        return msg;
    }

    @Override
    public String getTemplateUuid() {
        return uuid;
    }
}

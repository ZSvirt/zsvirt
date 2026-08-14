package org.zstack.header.managementnode;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/management-nodes/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateFactoryModeStateEvent.class,
        isAction = true
)
public class APIUpdateFactoryModeStateMsg extends APIMessage {
    @APIParam
    private Boolean factoryModeState;

    public Boolean getFactoryModeState() {
        return factoryModeState;
    }

    public void setFactoryModeState(Boolean factoryModeState) {
        this.factoryModeState = factoryModeState;
    }

    public static APIUpdateFactoryModeStateMsg __example__() {
        APIUpdateFactoryModeStateMsg msg = new APIUpdateFactoryModeStateMsg();
        msg.setFactoryModeState(true);
        return msg;
    }
}

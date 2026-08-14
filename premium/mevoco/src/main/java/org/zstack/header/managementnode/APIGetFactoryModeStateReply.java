package org.zstack.header.managementnode;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIReply;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIGetFactoryModeStateReply extends APIReply {
    private Boolean factoryModeState;

    public Boolean getFactoryModeState() {
        return factoryModeState;
    }

    public void setFactoryModeState(Boolean factoryModeState) {
        this.factoryModeState = factoryModeState;
    }

    public static APIGetFactoryModeStateReply __example__() {
        APIGetFactoryModeStateReply reply = new APIGetFactoryModeStateReply();
        reply.setFactoryModeState(true);
        return reply;
    }
}

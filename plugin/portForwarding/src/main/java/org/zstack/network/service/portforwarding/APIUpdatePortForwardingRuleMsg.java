package org.zstack.network.service.portforwarding;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/15/2015.
 */
@RestRequest(
        path = "/port-forwarding/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdatePortForwardingRuleEvent.class,
        isAction = true
)
public class APIUpdatePortForwardingRuleMsg extends APIMessage {
    @APIParam(resourceType = PortForwardingRuleVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
 
    public static APIUpdatePortForwardingRuleMsg __example__() {
        APIUpdatePortForwardingRuleMsg msg = new APIUpdatePortForwardingRuleMsg();
        msg.setUuid(uuid());
        msg.setName("pf2");
        msg.setDescription("new rule");
        return msg;
    }
}

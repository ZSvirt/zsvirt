package org.zstack.network.service.portforwarding;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryPortForwardingRuleReply.class, inventoryClass = PortForwardingRuleInventory.class)
@RestRequest(
        path = "/port-forwarding",
        method = HttpMethod.GET,
        optionalPaths = {"/port-forwarding/{uuid}"},
        responseClass = APIQueryPortForwardingRuleReply.class
)
public class APIQueryPortForwardingRuleMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("name=pf1", "state=Enabled");
    }

}

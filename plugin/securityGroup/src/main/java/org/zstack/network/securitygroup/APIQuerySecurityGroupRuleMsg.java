package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@AutoQuery(replyClass = APIQuerySecurityGroupRuleReply.class, inventoryClass = SecurityGroupRuleInventory.class)
@RestRequest(
        path = "/security-groups/rules",
        optionalPaths = {"/security-groups/rules/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySecurityGroupRuleReply.class
)
public class APIQuerySecurityGroupRuleMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("endPort=22", "state=Enabled");
    }

}

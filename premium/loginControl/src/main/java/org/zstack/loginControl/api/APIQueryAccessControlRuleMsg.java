package org.zstack.loginControl.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.loginControl.entity.AccessControlRuleInventory;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryAccessControlRuleReply.class, inventoryClass = AccessControlRuleInventory.class)
@RestRequest(
        path = "/login-control/access-control/rules",
        responseClass = APIQueryAccessControlRuleReply.class,
        method = HttpMethod.GET
)
public class APIQueryAccessControlRuleMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList(String.format("uuid=" + uuid()));
    }
}

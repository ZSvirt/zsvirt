package org.zstack.loginControl.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.loginControl.entity.AccessControlRuleInventory;
import org.zstack.loginControl.entity.AccessControlRuleVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@AutoQuery(replyClass = APIQueryAccessControlRuleReply.class, inventoryClass = AccessControlRuleInventory.class)
@RestRequest(
        path = "/login-control/access-control/rules",
        responseClass = APIQueryAccessControlRuleReply.class,
        method = HttpMethod.GET
)
public class APIQueryAccessControlRuleMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return list(String.format("uuid=" + uuid(AccessControlRuleVO.class)));
    }
}

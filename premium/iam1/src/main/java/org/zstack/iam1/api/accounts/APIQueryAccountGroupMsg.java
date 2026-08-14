package org.zstack.iam1.api.accounts;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.iam1.entity.accounts.AccountGroupInventory;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryAccountGroupReply.class, inventoryClass = AccountGroupInventory.class)
@RestRequest(
        path = "/account-groups",
        optionalPaths = {"/account-groups/{uuid}"},
        responseClass = APIQueryAccountGroupReply.class,
        method = HttpMethod.GET
)
public class APIQueryAccountGroupMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=my-group");
    }
}

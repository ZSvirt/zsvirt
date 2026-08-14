package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by frank on 2/25/2016.
 */
@AutoQuery(replyClass = APIQueryAccountResourceRefReply.class, inventoryClass = AccountResourceRefInventory.class)
@RestRequest(
        path = "/accounts/resources/refs",
        method = HttpMethod.GET,
        responseClass = APIQueryAccountResourceRefReply.class
)
public class APIQueryAccountResourceRefMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList(String.format("acountUuid=%s", uuid()));
    }

}

package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by frank on 7/14/2015.
 */
@AutoQuery(replyClass = APIQueryAccountReply.class, inventoryClass = AccountInventory.class)
@RestRequest(
        path = "/accounts",
        optionalPaths = {"/accounts/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAccountReply.class
)
public class APIQueryAccountMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("name=test");
    }

}

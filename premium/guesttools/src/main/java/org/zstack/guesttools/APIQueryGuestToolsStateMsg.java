package org.zstack.guesttools;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;
import static java.util.Arrays.asList;

@RestRequest(
        path = "/guesttools",
        optionalPaths = {"/guesttools/state"},
        responseClass = APIQueryGuestToolsStateReply.class,
        method = HttpMethod.GET
)
@AutoQuery(replyClass = APIQueryGuestToolsStateReply.class, inventoryClass = GuestToolsStateInventory.class)
public class APIQueryGuestToolsStateMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}

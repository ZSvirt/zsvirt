package org.zstack.mttyDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;
import static java.util.Arrays.asList;


@AutoQuery(replyClass = APIQueryMttyDeviceReply.class, inventoryClass = MttyDeviceInventory.class)
@RestRequest(
        path = "/mtty-devices",
        optionalPaths = {"/mtty-devices/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryMttyDeviceReply.class
)
public class APIQueryMttyDeviceMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}

package org.zstack.header.baremetal.pxeserver;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 4/20/17.
 */

@AutoQuery(replyClass = APIQueryBaremetalPxeServerReply.class, inventoryClass = BaremetalPxeServerInventory.class)
@RestRequest(
        path = "/baremetal/pxeservers",
        optionalPaths = {"/baremetal/pxeservers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryBaremetalPxeServerReply.class
)
public class APIQueryBaremetalPxeServerMsg extends APIQueryMessage{
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}

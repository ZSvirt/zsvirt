package org.zstack.header.baremetal.chassis;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 4/26/17.
 */
@AutoQuery(replyClass = APIQueryBaremetalChassisReply.class, inventoryClass = BaremetalChassisInventory.class)
@RestRequest(
        path = "/baremetal/chassis",
        optionalPaths = {"/baremetal/chassis/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryBaremetalChassisReply.class
)
public class APIQueryBaremetalChassisMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}

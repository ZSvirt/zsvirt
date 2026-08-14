package org.zstack.pciDevice.specification.mdev;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-04-30.
 */
@AutoQuery(replyClass = APIQueryMdevDeviceSpecReply.class, inventoryClass = MdevDeviceSpecInventory.class)
@RestRequest(
        path = "/mdev-device-specs",
        optionalPaths = {"/mdev-device-specs/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryMdevDeviceSpecReply.class
)
public class APIQueryMdevDeviceSpecMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}

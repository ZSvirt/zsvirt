package org.zstack.pciDevice.virtual.vfio_mdev;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by GuoYi on 2019-04-18.
 */
@AutoQuery(replyClass = APIQueryMdevDeviceReply.class, inventoryClass = MdevDeviceInventory.class)
@RestRequest(
        path = "/mdev-devices",
        optionalPaths = {"/mdev-devices/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryMdevDeviceReply.class
)
public class APIQueryMdevDeviceMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}

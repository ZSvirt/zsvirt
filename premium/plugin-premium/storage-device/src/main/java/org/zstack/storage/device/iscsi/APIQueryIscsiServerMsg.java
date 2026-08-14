package org.zstack.storage.device.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/storage-devices/iscsi/servers",
        optionalPaths = {"/storage-devices/iscsi",
                         "/storage-devices/iscsi/servers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryIscsiServerReply.class
)
@AutoQuery(replyClass = APIQueryIscsiServerReply.class, inventoryClass = IscsiServerInventory.class)
public class APIQueryIscsiServerMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}

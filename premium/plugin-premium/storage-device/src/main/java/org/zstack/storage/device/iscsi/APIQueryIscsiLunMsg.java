package org.zstack.storage.device.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/storage-devices/iscsi/luns",
        optionalPaths = {"/storage-devices/iscsi/luns/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryIscsiLunReply.class
)
@AutoQuery(replyClass = APIQueryIscsiLunReply.class, inventoryClass = IscsiLunInventory.class)
public class APIQueryIscsiLunMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}

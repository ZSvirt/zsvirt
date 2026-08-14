package org.zstack.storage.device.fibreChannel;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.storage.device.iscsi.IscsiLunInventory;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/storage-devices/fiber-channel/luns",
        optionalPaths = {"/storage-devices/fiber-channel/luns/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryFiberChannelLunReply.class
)
@AutoQuery(replyClass = APIQueryFiberChannelLunReply.class, inventoryClass = FiberChannelLunInventory.class)
public class APIQueryFiberChannelLunMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}

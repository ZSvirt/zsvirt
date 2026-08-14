package org.zstack.storage.device.fibreChannel;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.storage.device.iscsi.IscsiServerInventory;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/storage-devices/fiber-channel/controllers",
        optionalPaths = {"/storage-devices/fiber-channel",
                         "/storage-devices/fiber-channel/controllers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryFiberChannelStorageReply.class
)
@AutoQuery(replyClass = APIQueryFiberChannelStorageReply.class, inventoryClass = FiberChannelStorageInventory.class)
public class APIQueryFiberChannelStorageMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}

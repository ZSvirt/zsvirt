package org.zstack.storage.device.localRaid;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/storage-devices/local-raid/controllers",
        optionalPaths = {"/storage-devices/local-raid/controllers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryLocalRaidPhysicalDriveReply.class
)
@AutoQuery(replyClass = APIQueryLocalRaidPhysicalDriveReply.class, inventoryClass = RaidControllerInventory.class)
public class APIQueryLocalRaidControllerMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}

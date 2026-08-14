package org.zstack.storage.device.localRaid;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.storage.device.fibreChannel.FiberChannelLunInventory;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/storage-devices/local-raid/physical-drives",
        optionalPaths = {"/storage-devices/local-raid/physical-drives/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryLocalRaidPhysicalDriveReply.class
)
@AutoQuery(replyClass = APIQueryLocalRaidPhysicalDriveReply.class, inventoryClass = RaidPhysicalDriveInventory.class)
public class APIQueryLocalRaidPhysicalDriveMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}

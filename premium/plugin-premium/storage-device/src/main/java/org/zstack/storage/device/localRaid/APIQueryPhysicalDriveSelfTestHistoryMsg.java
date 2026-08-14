package org.zstack.storage.device.localRaid;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/storage-devices/local-raid/physical-drives/self-test",
        optionalPaths = {"/storage-devices/local-raid/physical-drives/{raidPhysicalDriveUuid}/self-test"},
        method = HttpMethod.GET,
        responseClass = APIQueryPhysicalDriveSelfTestHistoryReply.class
)
@AutoQuery(replyClass = APIQueryPhysicalDriveSelfTestHistoryReply.class, inventoryClass = PhysicalDriveSmartSelfTestHistoryInventory.class)
public class APIQueryPhysicalDriveSelfTestHistoryMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}

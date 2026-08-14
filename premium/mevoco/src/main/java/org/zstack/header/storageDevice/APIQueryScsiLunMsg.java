package org.zstack.header.storageDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/storage-devices/scsi-lun/luns",
        optionalPaths = {"/storage-devices/scsi-lun/luns/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryScsiLunReply.class
)
@AutoQuery(replyClass = APIQueryScsiLunReply.class, inventoryClass = ScsiLunInventory.class)
public class APIQueryScsiLunMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}

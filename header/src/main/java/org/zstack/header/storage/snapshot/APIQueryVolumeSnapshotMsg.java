package org.zstack.header.storage.snapshot;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 */
@AutoQuery(replyClass = APIQueryVolumeSnapshotReply.class, inventoryClass = VolumeSnapshotInventory.class)
@RestRequest(
        path = "/volume-snapshots",
        optionalPaths = {"/volume-snapshots/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVolumeSnapshotReply.class
)
public class APIQueryVolumeSnapshotMsg extends APIQueryMessage {
 
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }

}

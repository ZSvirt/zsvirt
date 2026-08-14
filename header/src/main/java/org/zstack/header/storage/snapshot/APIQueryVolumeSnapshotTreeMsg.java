package org.zstack.header.storage.snapshot;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 */
@AutoQuery(replyClass = APIQueryVolumeSnapshotTreeReply.class, inventoryClass = VolumeSnapshotTreeInventory.class)
@RestRequest(
        path = "/volume-snapshots/trees",
        optionalPaths = {"/volume-snapshots/trees/{uuid}"},
        responseClass = APIQueryVolumeSnapshotTreeReply.class,
        method = HttpMethod.GET
)
public class APIQueryVolumeSnapshotTreeMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }

}

package org.zstack.header.storage.snapshot.group;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/7/11.
 */
@AutoQuery(replyClass = APIQueryVolumeSnapshotGroupReply.class, inventoryClass = VolumeSnapshotGroupInventory.class)
@RestRequest(
        path = "/volume-snapshots/group",
        optionalPaths = {"/volume-snapshots/group/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVolumeSnapshotGroupReply.class
)
public class APIQueryVolumeSnapshotGroupMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}

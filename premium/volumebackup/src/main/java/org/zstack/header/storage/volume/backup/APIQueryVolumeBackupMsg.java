package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.VolumeBackupInventory;

import java.util.Collections;
import java.util.List;

@AutoQuery(replyClass = APIQueryVolumeBackupReply.class, inventoryClass = VolumeBackupInventory.class)
@RestRequest(
        path = "/volume-backups",
        optionalPaths = {"/volume-backups/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryVolumeBackupReply.class
)
public class APIQueryVolumeBackupMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}

package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@AutoQuery(replyClass = APIQueryBackupStorageReply.class, inventoryClass = BackupStorageInventory.class)
@RestRequest(
        path = "/backup-storage",
        optionalPaths = {"/backup-storage/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryBackupStorageReply.class
)
public class APIQueryBackupStorageMsg extends APIQueryMessage {

 
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }

}

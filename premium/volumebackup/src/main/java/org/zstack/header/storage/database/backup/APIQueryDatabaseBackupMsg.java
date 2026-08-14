package org.zstack.header.storage.database.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@AutoQuery(replyClass = APIQueryDatabaseBackupReply.class, inventoryClass = DatabaseBackupInventory.class)
@RestRequest(
        path = "/database-backups",
        optionalPaths = {"/database-backups/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryDatabaseBackupReply.class
)
public class APIQueryDatabaseBackupMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }
}

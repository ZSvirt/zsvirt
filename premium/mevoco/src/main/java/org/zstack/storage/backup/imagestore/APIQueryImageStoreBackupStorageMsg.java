package org.zstack.storage.backup.imagestore;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@AutoQuery(replyClass = APIQueryImageStoreBackupStorageReply.class, inventoryClass = ImageStoreBackupStorageInventory.class)
@RestRequest(
        path = "/backup-storage/image-store",
        optionalPaths = {"/backup-storage/image-store/{uuid}"},
        responseClass = APIQueryImageStoreBackupStorageReply.class,
        method = HttpMethod.GET
)
public class APIQueryImageStoreBackupStorageMsg extends APIQueryMessage {
 
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }

}

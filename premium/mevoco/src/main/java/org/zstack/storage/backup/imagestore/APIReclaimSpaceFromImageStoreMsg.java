package org.zstack.storage.backup.imagestore;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;

/**
 * Created by david on 2/25/17.
 */
@RestRequest(
        path = "/backup-storage/image-store/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIReclaimSpaceFromImageStoreEvent.class
)
public class APIReclaimSpaceFromImageStoreMsg extends APIMessage {
    @APIParam(resourceType = BackupStorageVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIReclaimSpaceFromImageStoreMsg __example__() {
        APIReclaimSpaceFromImageStoreMsg msg = new APIReclaimSpaceFromImageStoreMsg();

        msg.setUuid(uuid());

        return msg;
    }
}

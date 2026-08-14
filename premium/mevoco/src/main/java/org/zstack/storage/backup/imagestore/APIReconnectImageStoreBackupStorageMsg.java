package org.zstack.storage.backup.imagestore;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;
import org.zstack.header.storage.backup.BackupStorageVO;

@RestRequest(
        path = "/backup-storage/image-store/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIReconnectImageStoreBackupStorageEvent.class
)
public class APIReconnectImageStoreBackupStorageMsg extends APIMessage implements
        BackupStorageMessage {
    @APIParam(resourceType = BackupStorageVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return uuid;
    }

    public void setUuid(String backupStorageUuid) {
        this.uuid = backupStorageUuid;
    }
 
    public static APIReconnectImageStoreBackupStorageMsg __example__() {
        APIReconnectImageStoreBackupStorageMsg msg = new APIReconnectImageStoreBackupStorageMsg();

        msg.setUuid(uuid());

        return msg;
    }

}

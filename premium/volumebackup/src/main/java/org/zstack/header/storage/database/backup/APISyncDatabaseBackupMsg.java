package org.zstack.header.storage.database.backup;

import org.springframework.http.HttpMethod;
import org.zstack.core.Platform;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;

@RestRequest(
        path = "/database-backups/imageStore/{imageStoreUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISyncDatabaseBackupEvent.class
)
public class APISyncDatabaseBackupMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = ImageStoreBackupStorageVO.class)
    private String imageStoreUuid;

    public String getImageStoreUuid() {
        return imageStoreUuid;
    }

    public void setImageStoreUuid(String imageStoreUuid) {
        this.imageStoreUuid = imageStoreUuid;
    }

    public static APISyncDatabaseBackupMsg __example__() {
        APISyncDatabaseBackupMsg msg = new APISyncDatabaseBackupMsg();
        msg.setImageStoreUuid(uuid(ImageStoreBackupStorageVO.class));
        return msg;
    }

    @Override
    public String getBackupStorageUuid() {
        return imageStoreUuid;
    }
}

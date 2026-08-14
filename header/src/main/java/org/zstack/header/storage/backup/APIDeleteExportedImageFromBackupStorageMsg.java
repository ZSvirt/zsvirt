package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by david on 8/31/16.
 */
@RestRequest(
        path = "/backup-storage/{backupStorageUuid}/exported-images/{imageUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteExportedImageFromBackupStorageEvent.class
)
public class APIDeleteExportedImageFromBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = BackupStorageVO.class)
    private String backupStorageUuid;

    @APIParam(resourceType = ImageVO.class)
    private String imageUuid;

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }
 
    public static APIDeleteExportedImageFromBackupStorageMsg __example__() {
        APIDeleteExportedImageFromBackupStorageMsg msg = new APIDeleteExportedImageFromBackupStorageMsg();

        msg.setBackupStorageUuid(uuid());
        msg.setImageUuid(uuid());

        return msg;
    }
}

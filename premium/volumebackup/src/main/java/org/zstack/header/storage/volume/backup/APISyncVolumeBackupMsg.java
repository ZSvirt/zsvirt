package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;

/**
 * Created by MaJin on 2019/4/26.
 */
@RestRequest(
        path = "/volume-backups/imageStore/{imageStoreUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISyncVolumeBackupEvent.class
)
public class APISyncVolumeBackupMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = ImageStoreBackupStorageVO.class)
    private String imageStoreUuid;

    public String getImageStoreUuid() {
        return imageStoreUuid;
    }

    public void setImageStoreUuid(String imageStoreUuid) {
        this.imageStoreUuid = imageStoreUuid;
    }

    public static APISyncVolumeBackupMsg __example__() {
        APISyncVolumeBackupMsg msg = new APISyncVolumeBackupMsg();
        msg.setImageStoreUuid(uuid(ImageStoreBackupStorageVO.class));
        return msg;
    }

    @Override
    public String getBackupStorageUuid() {
        return imageStoreUuid;
    }
}


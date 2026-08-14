package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;

/**
 * Created by MaJin on 2019/5/6.
 */
@RestRequest(
        path = "/vm-backups/imageStore/{imageStoreUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISyncVmBackupEvent.class
)
public class APISyncVmBackupMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = ImageStoreBackupStorageVO.class)
    private String imageStoreUuid;

    public String getImageStoreUuid() {
        return imageStoreUuid;
    }

    public void setImageStoreUuid(String imageStoreUuid) {
        this.imageStoreUuid = imageStoreUuid;
    }

    public static APISyncVmBackupMsg __example__() {
        APISyncVmBackupMsg msg = new APISyncVmBackupMsg();
        msg.setImageStoreUuid(uuid(ImageStoreBackupStorageVO.class));
        return msg;
    }

    @Override
    public String getBackupStorageUuid() {
        return imageStoreUuid;
    }
}

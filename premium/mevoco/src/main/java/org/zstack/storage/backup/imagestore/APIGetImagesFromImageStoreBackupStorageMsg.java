package org.zstack.storage.backup.imagestore;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;

/**
 * Created by mingjian.deng on 2017/8/31.
 */
@RestRequest(
        path = "/backup-storage/{uuid}/image-store",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIGetImagesFromImageStoreBackupStorageReply.class
)
public class APIGetImagesFromImageStoreBackupStorageMsg extends APISyncCallMessage {
    @APIParam(resourceType = BackupStorageVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIGetImagesFromImageStoreBackupStorageMsg __example__() {
        APIGetImagesFromImageStoreBackupStorageMsg msg = new APIGetImagesFromImageStoreBackupStorageMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

package org.zstack.storage.backup.imagestore;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/backup-storage/image-store/{imageStoreUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APISyncImageEvent.class
)
public class APISyncImageMsg extends APIMessage {
    @APIParam(resourceType = ImageStoreBackupStorageVO.class)
    private String imageStoreUuid;

    public String getImageStoreUuid() {
        return imageStoreUuid;
    }

    public void setImageStoreUuid(String imageStoreUuid) {
        this.imageStoreUuid = imageStoreUuid;
    }

    public static APISyncImageMsg __example__() {
        APISyncImageMsg msg = new APISyncImageMsg();
        msg.setImageStoreUuid(uuid(ImageStoreBackupStorageVO.class));
        return msg;
    }
}

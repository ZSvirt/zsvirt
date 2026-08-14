package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestRequest(
        path = "/backup-storage/trash",
        method = HttpMethod.GET,
        responseClass = APIGetTrashOnBackupStorageReply.class
)
public class APIGetTrashOnBackupStorageMsg extends APISyncCallMessage implements BackupStorageMessage {
    @APIParam(resourceType = BackupStorageVO.class)
    private String uuid;
    @APIParam(required = false)
    private String resourceUuid;
    @APIParam(required = false)
    private String resourceType;
    @APIParam(required = false, validValues = {"MigrateImage"})
    private String trashType;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return uuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getTrashType() {
        return trashType;
    }

    public void setTrashType(String trashType) {
        this.trashType = trashType;
    }

    public static APIGetTrashOnBackupStorageMsg __example__() {
        APIGetTrashOnBackupStorageMsg msg = new APIGetTrashOnBackupStorageMsg();
        msg.setUuid(uuid());

        return msg;
    }
}

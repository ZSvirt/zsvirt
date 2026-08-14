package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestRequest(
        path = "/primary-storage/trash",
        method = HttpMethod.GET,
        responseClass = APIGetTrashOnPrimaryStorageReply.class
)
public class APIGetTrashOnPrimaryStorageMsg extends APISyncCallMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String uuid;
    @APIParam(required = false)
    private String resourceUuid;
    @APIParam(required = false)
    private String resourceType;
    @APIParam(required = false, validValues = {"MigrateVolume", "MigrateVolumeSnapshot", "RevertVolume", "VolumeSnapshot"})
    private String trashType;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
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

    public static APIGetTrashOnPrimaryStorageMsg __example__() {
        APIGetTrashOnPrimaryStorageMsg msg = new APIGetTrashOnPrimaryStorageMsg();
        msg.setUuid(uuid());

        return msg;
    }
}

package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;

import java.util.Collections;
import java.util.List;

/**
 * Created by frank on 11/15/2015.
 */
@RestRequest(
        path = "/images/{imageUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIExpungeImageEvent.class,
        isAction = true
)
public class APIExpungeImageMsg extends APIMessage implements ImageMessage {
    @APIParam(required = false, resourceType = ImageVO.class)
    // used for cloudformation
    private String uuid;
    @APIParam(resourceType = ImageVO.class)
    private String imageUuid;
    @APIParam(required = false, nonempty = true, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;

    @Override
    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getUuid() {
        return imageUuid != null ? imageUuid : uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
        this.imageUuid = uuid;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }
 
    public static APIExpungeImageMsg __example__() {
        APIExpungeImageMsg msg = new APIExpungeImageMsg();

        msg.setBackupStorageUuids(Collections.singletonList(uuid()));
        msg.setImageUuid(uuid());

        return msg;
    }
}

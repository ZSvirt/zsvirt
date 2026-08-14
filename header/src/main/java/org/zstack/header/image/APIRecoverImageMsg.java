package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

/**
 * Created by frank on 11/15/2015.
 */
@RestRequest(
        path = "/images/{imageUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIRecoverImageEvent.class,
        isAction = true
)
public class APIRecoverImageMsg extends APIMessage implements ImageMessage {
    @APIParam(resourceType = ImageVO.class)
    private String imageUuid;
    private List<String> backupStorageUuids;

    @Override
    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }
 
    public static APIRecoverImageMsg __example__() {
        APIRecoverImageMsg msg = new APIRecoverImageMsg();

        msg.setBackupStorageUuids(Collections.singletonList(uuid()));
        msg.setImageUuid(uuid());

        return msg;
    }
}

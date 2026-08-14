package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/primary-storage/{uuid}/storagetrash/actions",
        isAction = true,
        responseClass = APICleanUpStorageTrashOnPrimaryStorageEvent.class,
        method = HttpMethod.PUT
)
public class APICleanUpStorageTrashOnPrimaryStorageMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String uuid;
    @APIParam(required = false)
    private boolean force;

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public static APICleanUpStorageTrashOnPrimaryStorageMsg __example__() {
        APICleanUpStorageTrashOnPrimaryStorageMsg msg = new APICleanUpStorageTrashOnPrimaryStorageMsg();
        msg.setUuid(uuid(PrimaryStorageVO.class));
        msg.setForce(true);
        return msg;
    }
}

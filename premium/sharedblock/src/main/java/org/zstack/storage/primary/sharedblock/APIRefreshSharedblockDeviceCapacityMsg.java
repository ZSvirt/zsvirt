package org.zstack.storage.primary.sharedblock;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

@RestRequest(
        path = "/primary-storage/sharedblockgroup/{sharedBlockGroupUuid}/sharedblocks/{uuid}",
        method = HttpMethod.POST,
        responseClass = APIRefreshSharedBlockDeviceCapacityEvent.class,
        parameterName = "params"
)
public class APIRefreshSharedblockDeviceCapacityMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(required = false, resourceType = SharedBlockVO.class)
    private String uuid;

    @APIParam(resourceType = SharedBlockGroupVO.class)
    private String sharedBlockGroupUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public APIRefreshSharedblockDeviceCapacityMsg() {
    }

    public String getSharedBlockGroupUuid() {
        return sharedBlockGroupUuid;
    }

    public void setSharedBlockGroupUuid(String uuid) {
        this.sharedBlockGroupUuid = uuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return sharedBlockGroupUuid;
    }

    public static APIRefreshSharedblockDeviceCapacityMsg __example__() {
        APIRefreshSharedblockDeviceCapacityMsg msg = new APIRefreshSharedblockDeviceCapacityMsg();
        msg.setSharedBlockGroupUuid(uuid());
        msg.setUuid(uuid());
        return msg;
    }
}

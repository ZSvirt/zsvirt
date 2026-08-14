package org.zstack.storage.primary.sharedblock;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;

import java.util.ArrayList;
import java.util.List;

@RestRequest(
        path = "/primary-storage/sharedblockgroup/{uuid}/sharedblocks",
        method = HttpMethod.POST,
        responseClass = APIAddSharedBlockToSharedBlockGroupEvent.class,
        parameterName = "params"
)
public class APIAddSharedBlockToSharedBlockGroupMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam
    private String diskUuid;

    @APIParam(resourceType = SharedBlockGroupVO.class)
    private String uuid;

    public APIAddSharedBlockToSharedBlockGroupMsg() {
    }

    public String getDiskUuid() {
        return diskUuid;
    }

    public void setDiskUuid(String diskUuid) {
        this.diskUuid = diskUuid;
    }

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

    public static APIAddSharedBlockToSharedBlockGroupMsg __example__() {
        APIAddSharedBlockToSharedBlockGroupMsg msg = new APIAddSharedBlockToSharedBlockGroupMsg();
        msg.setUuid(uuid());
        msg.setDiskUuid(uuid());
        return msg;
    }
}

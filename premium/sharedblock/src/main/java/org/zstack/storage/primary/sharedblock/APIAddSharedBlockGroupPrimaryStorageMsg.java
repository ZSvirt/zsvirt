package org.zstack.storage.primary.sharedblock;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;

import java.util.ArrayList;
import java.util.List;

@OverriddenApiParams({
        @OverriddenApiParam(field = "url", param = @APIParam(maxLength = 2048, required = false))
})
@TagResourceType(PrimaryStorageVO.class)
@RestRequest(
        path = "/primary-storage/sharedblockgroup",
        method = HttpMethod.POST,
        responseClass = APIAddPrimaryStorageEvent.class,
        parameterName = "params"
)
public class APIAddSharedBlockGroupPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
    @APIParam
    private List<String> diskUuids;

    public APIAddSharedBlockGroupPrimaryStorageMsg() {
        this.setType(SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE);
    }

    @Override
    public String getType() {
        return SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE;
    }

    public List<String> getDiskUuids() {
        return diskUuids;
    }

    public void setDiskUuids(List<String> diskUuids) {
        this.diskUuids = diskUuids;
    }

    public static APIAddSharedBlockGroupPrimaryStorageMsg __example__() {
        APIAddSharedBlockGroupPrimaryStorageMsg msg = new APIAddSharedBlockGroupPrimaryStorageMsg();
        msg.setName("shared-block-group-1");
        List<String> uuids = new ArrayList<>();
        uuids.add(uuid());
        msg.setDiskUuids(uuids);
        msg.setZoneUuid(uuid());
        return msg;
    }
}

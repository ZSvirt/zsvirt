package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

/**
 * Created by LiangHanYu on 2022/7/5 17:44
 */
@RestRequest(
        path = "/memory-snapshots/group/reference",
        method = HttpMethod.GET,
        responseClass = APIGetMemorySnapshotGroupReferenceReply.class
)
public class APIGetMemorySnapshotGroupReferenceMsg extends APISyncCallMessage {
    @APIParam(resourceType = ResourceVO.class)
    private String resourceUuid;

    @APIParam
    private String resourceType;

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

    public static APIGetMemorySnapshotGroupReferenceMsg __example__() {
        APIGetMemorySnapshotGroupReferenceMsg msg = new APIGetMemorySnapshotGroupReferenceMsg();
        msg.setResourceUuid(uuid());
        msg.setResourceType(L3NetworkVO.class.getSimpleName());
        return msg;
    }
}

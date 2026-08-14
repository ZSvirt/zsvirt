package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/primary-storage/stranger",
        method = HttpMethod.GET,
        responseClass = APIDiscoverStrangePrimaryStorageReply.class
)
public class APIDiscoverStrangePrimaryStorageMsg extends APISyncCallMessage {
    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public static APIDiscoverStrangePrimaryStorageMsg __example__() {
        APIDiscoverStrangePrimaryStorageMsg msg = new APIDiscoverStrangePrimaryStorageMsg();
        msg.setClusterUuid(uuid());
        return msg;
    }
}

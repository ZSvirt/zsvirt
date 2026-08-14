package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

import java.util.Collections;
import java.util.List;

/**
 */
@RestRequest(
        path = "/primary-storage/capacities",
        method = HttpMethod.GET,
        responseClass = APIGetPrimaryStorageCapacityReply.class
)
public class APIGetPrimaryStorageCapacityMsg extends APISyncCallMessage {
    @APIParam(required = false, resourceType = ZoneVO.class)
    private List<String> zoneUuids;
    @APIParam(required = false, resourceType = ClusterVO.class)
    private List<String> clusterUuids;
    @APIParam(required = false, resourceType = PrimaryStorageVO.class)
    private List<String> primaryStorageUuids;
    private boolean all;

    public List<String> getZoneUuids() {
        return zoneUuids;
    }

    public void setZoneUuids(List<String> zoneUuids) {
        this.zoneUuids = zoneUuids;
    }

    public List<String> getClusterUuids() {
        return clusterUuids;
    }

    public void setClusterUuids(List<String> clusterUuids) {
        this.clusterUuids = clusterUuids;
    }

    public List<String> getPrimaryStorageUuids() {
        return primaryStorageUuids;
    }

    public void setPrimaryStorageUuids(List<String> primaryStorageUuids) {
        this.primaryStorageUuids = primaryStorageUuids;
    }

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }
 
    public static APIGetPrimaryStorageCapacityMsg __example__() {
        APIGetPrimaryStorageCapacityMsg msg = new APIGetPrimaryStorageCapacityMsg();

        msg.setPrimaryStorageUuids(Collections.singletonList(uuid()));

        return msg;
    }

}

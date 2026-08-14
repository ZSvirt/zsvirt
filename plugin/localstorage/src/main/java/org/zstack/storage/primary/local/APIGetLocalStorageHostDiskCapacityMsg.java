package org.zstack.storage.primary.local;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;

/**
 * Created by frank on 10/15/2015.
 */
@RestRequest(
        path = "/primary-storage/local-storage/{primaryStorageUuid}/capacities",
        method = HttpMethod.GET,
        responseClass = APIGetLocalStorageHostDiskCapacityReply.class
)
public class APIGetLocalStorageHostDiskCapacityMsg extends APISyncCallMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = HostVO.class, required = false)
    private String hostUuid;
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
 
    public static APIGetLocalStorageHostDiskCapacityMsg __example__() {
        APIGetLocalStorageHostDiskCapacityMsg msg = new APIGetLocalStorageHostDiskCapacityMsg();

        msg.setHostUuid(uuid());
        msg.setPrimaryStorageUuid(uuid());

        return msg;
    }

}

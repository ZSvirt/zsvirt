package org.zstack.storage.device.localRaid;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2018/8/2
 */
@RestRequest(
        path = "/storage-devices/local-raid/physical-drives/{uuid}/smart",
        method = HttpMethod.GET,
        responseClass = APIGetLocalRaidPhysicalDriveSmartReply.class
)
public class APIGetLocalRaidPhysicalDriveSmartMsg extends APISyncCallMessage {
    @APIParam(resourceType = RaidPhysicalDriveVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIGetLocalRaidPhysicalDriveSmartMsg __example__() {
        APIGetLocalRaidPhysicalDriveSmartMsg msg = new APIGetLocalRaidPhysicalDriveSmartMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

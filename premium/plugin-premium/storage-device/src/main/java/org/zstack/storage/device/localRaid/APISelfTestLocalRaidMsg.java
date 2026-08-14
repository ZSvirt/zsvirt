package org.zstack.storage.device.localRaid;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/storage-devices/local-raid/physical-drives/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APISelfTestLocalRaidEvent.class,
        isAction = true
)
public class APISelfTestLocalRaidMsg extends APIMessage {
    @APIParam(resourceType = RaidPhysicalDriveVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APISelfTestLocalRaidMsg __example__() {
        APISelfTestLocalRaidMsg msg = new APISelfTestLocalRaidMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

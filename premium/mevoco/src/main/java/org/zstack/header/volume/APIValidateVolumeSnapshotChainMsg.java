package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/volumes/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIValidateVolumeSnapshotChainEvent.class,
        isAction = true
)
public class APIValidateVolumeSnapshotChainMsg extends APIMessage {
    @APIParam(resourceType = VolumeVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIValidateVolumeSnapshotChainMsg __example__() {
        APIValidateVolumeSnapshotChainMsg msg = new APIValidateVolumeSnapshotChainMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

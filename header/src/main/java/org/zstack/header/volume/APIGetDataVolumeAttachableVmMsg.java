package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 */
@RestRequest(
        path = "/volumes/{volumeUuid}/candidate-vm-instances",
        method = HttpMethod.GET,
        responseClass = APIGetDataVolumeAttachableVmReply.class
)
public class APIGetDataVolumeAttachableVmMsg extends APISyncCallMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class)
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
 
    public static APIGetDataVolumeAttachableVmMsg __example__() {
        APIGetDataVolumeAttachableVmMsg msg = new APIGetDataVolumeAttachableVmMsg();
        msg.setVolumeUuid(uuid());

        return msg;
    }

}

package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/volumes/{volumeUuid}/hosts",
        method = HttpMethod.DELETE,
        responseClass = APIDetachDataVolumeFromHostEvent.class
)
public class APIDetachDataVolumeFromHostMsg extends APIMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class)
    private String volumeUuid;

    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public static APIDetachDataVolumeFromHostMsg __example__() {
        APIDetachDataVolumeFromHostMsg msg = new APIDetachDataVolumeFromHostMsg();
        msg.setVolumeUuid(uuid());
        msg.setHostUuid(uuid());
        return msg;
    }
}

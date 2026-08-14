package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/volumes/{volumeUuid}/hosts/{hostUuid}",
        method = HttpMethod.POST,
        responseClass = APIAttachDataVolumeToHostEvent.class,
        parameterName = "params"
)
public class APIAttachDataVolumeToHostMsg extends APIMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class)
    private String volumeUuid;

    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    @APIParam(maxLength = 512)
    private String mountPath;

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

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    public static APIAttachDataVolumeToHostMsg __example__() {
        APIAttachDataVolumeToHostMsg msg = new APIAttachDataVolumeToHostMsg();
        msg.setVolumeUuid(uuid());
        msg.setHostUuid(uuid());
        msg.setMountPath("/test/mount/path");
        return msg;
    }
}

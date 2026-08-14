package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmClockTrackEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "uuid")
public class APISetVmClockTrackMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;
    @APIParam(validValues = {"guest", "host"})
    private String track;
    @APIParam(required = false)
    private Boolean syncAfterVMResume;
    @APIParam(validValues = {"0", "60", "600", "1800", "3600", "7200", "21600", "43200", "86400"}, required = false)
    private Integer intervalInSeconds;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public Boolean isSyncAfterVMResume() {
        return syncAfterVMResume;
    }

    public void setSyncAfterVMResume(boolean syncAfterVMResume) {
        this.syncAfterVMResume = syncAfterVMResume;
    }

    public Integer getIntervalInSeconds() {
        return intervalInSeconds;
    }

    public void setIntervalInSeconds(Integer intervalInSeconds) {
        this.intervalInSeconds = intervalInSeconds;
    }

    public static APISetVmClockTrackMsg __example__() {
        APISetVmClockTrackMsg msg = new APISetVmClockTrackMsg();
        msg.uuid = uuid();
        msg.track = "guest";
        msg.syncAfterVMResume = true;
        msg.intervalInSeconds = 60;
        return msg;
    }
}

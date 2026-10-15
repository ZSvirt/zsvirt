package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/volumes/shrink/{uuid}/actions",
        responseClass = APIShrinkVolumeEvent.class,
        isAction = true,
        method = HttpMethod.PUT
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VolumeUuidToVmUuidResolver", field = "uuid")
public class APIShrinkVolumeMsg extends APIMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }

    public static APIShrinkVolumeMsg __example__() {
        APIShrinkVolumeMsg msg = new APIShrinkVolumeMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

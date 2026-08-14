package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by xing5 on 2016/4/24.
 */
@RestRequest(
        path = "/volumes/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APISyncVolumeSizeEvent.class,
        isAction = true
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VolumeUuidToVmUuidResolver", field = "uuid")
public class APISyncVolumeSizeMsg extends APIMessage implements VolumeMessage {
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
 
    public static APISyncVolumeSizeMsg __example__() {
        APISyncVolumeSizeMsg msg = new APISyncVolumeSizeMsg();
        msg.setUuid(uuid());

        return msg;
    }
}

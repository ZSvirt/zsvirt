package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by frank on 6/14/2015.
 */
@RestRequest(
        path = "/volumes/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateVolumeEvent.class,
        isAction = true
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VolumeUuidToVmUuidResolver", field = "uuid")
public class APIUpdateVolumeMsg extends APIMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }
 
    public static APIUpdateVolumeMsg __example__() {
        APIUpdateVolumeMsg msg = new APIUpdateVolumeMsg();
        msg.setName("volume-1");
        msg.setDescription("data-volume");
        msg.setUuid(uuid());

        return msg;
    }
}

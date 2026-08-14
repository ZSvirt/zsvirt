
package org.zstack.header.volume.block;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.storage.volume.block.BlockVolumeMessage;

@RestRequest(
        path = "/block-volumes/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateBlockVolumeEvent.class,
        isAction = true
)
public class APIUpdateBlockVolumeMsg extends APIMessage implements BlockVolumeMessage {
    @APIParam(resourceType = BlockVolumeVO.class)
    private String uuid;
    
    @APIParam(required = false)
    private String name;
    
    @APIParam(required = false)
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
    
    public static APIUpdateBlockVolumeMsg __example__() {
        APIUpdateBlockVolumeMsg msg = new APIUpdateBlockVolumeMsg();
        msg.setUuid(uuid());
        msg.setName("example");
        return msg;
    }

    @Override
    public String getBlockVolumeUuid() {
        return getUuid();
    }
}

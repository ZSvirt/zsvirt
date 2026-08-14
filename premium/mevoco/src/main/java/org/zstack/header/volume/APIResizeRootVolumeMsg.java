package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by kayo on 2017/9/12.
 */
@RestRequest(
        path = "/volumes/resize/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIResizeRootVolumeEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VolumeUuidToVmUuidResolver", field = "uuid")
public class APIResizeRootVolumeMsg extends APIMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class, required = false)
    private String uuid;

    @APIParam(resourceType = VmInstanceVO.class, required = false)
    private String vmInstanceUuid;

    @APIParam
    private long size;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public static APIResizeRootVolumeMsg __example__() {
        APIResizeRootVolumeMsg msg = new APIResizeRootVolumeMsg();
        msg.setUuid(uuid());
        msg.setSize(10000000L);
        return msg;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }
}

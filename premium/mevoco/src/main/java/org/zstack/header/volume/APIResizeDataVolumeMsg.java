package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;
/**
 * Created by camile on 2017/11/3.
 */
@RestRequest(
        path = "/volumes/data/resize/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIResizeDataVolumeEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VolumeUuidToVmUuidResolver", field = "uuid")
public class APIResizeDataVolumeMsg extends APIMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class)
    private String uuid;

    @APIParam
    private long size;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public static APIResizeDataVolumeMsg __example__() {
        APIResizeDataVolumeMsg msg = new APIResizeDataVolumeMsg();
        msg.setUuid(uuid());
        msg.setSize(10000000L);
        return msg;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }
}

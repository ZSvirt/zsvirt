package org.zstack.storage.primary.sharedblock;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.volume.VolumeVO;

@RestRequest(
        path = "/primary-storage/sharedblockgroup/volumes/{volumeUuid}/actions",
        responseClass = APIExtendSharedBlockVolumeEvent.class,
        isAction = true,
        method = HttpMethod.PUT
)
public class APIExtendSharedBlockVolumeMsg extends APIMessage implements PrimaryStorageMessage {
    @APINoSee
    private String primaryStorageUuid;

    @APIParam(resourceType = VolumeVO.class)
    private String volumeUuid;

    @APIParam(numberRange = {1, Long.MAX_VALUE}, numberRangeUnit = {"byte", "bytes"})
    private long size;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public static APIExtendSharedBlockVolumeMsg __example__() {
        APIExtendSharedBlockVolumeMsg msg = new APIExtendSharedBlockVolumeMsg();
        msg.setVolumeUuid(uuid());
        msg.setSize(10L * 1024 * 1024 * 1024);
        return msg;
    }
}

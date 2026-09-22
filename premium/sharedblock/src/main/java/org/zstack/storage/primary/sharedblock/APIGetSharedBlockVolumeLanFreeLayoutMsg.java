package org.zstack.storage.primary.sharedblock;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.volume.VolumeVO;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/primary-storage/sharedblockgroup/volume-lan-free-layouts",
        method = HttpMethod.POST,
        responseClass = APIGetSharedBlockVolumeLanFreeLayoutReply.class,
        parameterName = "params"
)
public class APIGetSharedBlockVolumeLanFreeLayoutMsg extends APISyncCallMessage {
    @APIParam(nonempty = true, nullElements = false, emptyString = false,
            resourceType = VolumeVO.class)
    private List<String> volumeUuids;

    public List<String> getVolumeUuids() {
        return volumeUuids;
    }

    public void setVolumeUuids(List<String> volumeUuids) {
        this.volumeUuids = volumeUuids;
    }

    public static APIGetSharedBlockVolumeLanFreeLayoutMsg __example__() {
        APIGetSharedBlockVolumeLanFreeLayoutMsg msg =
                new APIGetSharedBlockVolumeLanFreeLayoutMsg();
        msg.setVolumeUuids(Collections.singletonList(uuid()));
        return msg;
    }
}

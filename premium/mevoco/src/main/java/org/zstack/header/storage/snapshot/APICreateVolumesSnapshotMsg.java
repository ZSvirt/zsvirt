package org.zstack.header.storage.snapshot;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.NoDoc;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;
import org.zstack.header.volume.VolumeMessage;
import org.zstack.header.volume.VolumeVO;

import java.util.Arrays;
import java.util.List;

/**
 * Create by weiwang at 2018/6/8
 */
@RestRequest(
        path = "/volumes/volume-snapshots",
        method = HttpMethod.POST,
        responseClass = APICreateVolumesSnapshotEvent.class,
        parameterName = "params"
)
@NoDoc
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "VolumeUuidsToVmUuidResolver", field = "volumeUuids")
public class APICreateVolumesSnapshotMsg extends APIMessage implements APIAuditor, VolumeMessage {
    @APIParam(resourceType = VolumeVO.class)
    private List<String> volumeUuids;

    public List<String> getVolumeUuids() {
        return volumeUuids;
    }

    public void setVolumeUuids(List<String> volumeUuids) {
        this.volumeUuids = volumeUuids;
    }

    public static APICreateVolumesSnapshotMsg __example__() {
        APICreateVolumesSnapshotMsg msg = new APICreateVolumesSnapshotMsg();
        msg.setVolumeUuids(Arrays.asList(uuid(), uuid()));

        return msg;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuids.get(0);
    }

    // NOTE(weiw): since this api will not be public, the audit is buggy for now
    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateVolumesSnapshotEvent)rsp).getInventories().get(0).getUuid() : "", VolumeSnapshotVO.class);
    }
}

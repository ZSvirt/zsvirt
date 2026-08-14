package org.zstack.header.storage.snapshot;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by lining on 2019/5/14.
 */
@RestRequest(
        path = "/volume-snapshots/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIGetVolumeSnapshotSizeEvent.class,
        isAction = true
)
public class APIGetVolumeSnapshotSizeMsg extends APIMessage implements VolumeSnapshotMessage {
    @APIParam(resourceType = VolumeSnapshotVO.class)
    private String uuid;

    @APINoSee
    private String volumeUuid;

    @APINoSee
    private String treeUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getSnapshotUuid() {
        return uuid;
    }

    @Override
    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    @Override
    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }

    @Override
    public String getTreeUuid() {
        return treeUuid;
    }

    public static APIGetVolumeSnapshotSizeMsg __example__() {
        APIGetVolumeSnapshotSizeMsg msg = new APIGetVolumeSnapshotSizeMsg();
        msg.setUuid(uuid());

        return msg;
    }
}

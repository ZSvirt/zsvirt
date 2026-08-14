package org.zstack.header.storage.snapshot;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.concurrent.TimeUnit;

/**
 * @ Author : yh.w
 * @ Date   : Created in 13:24 2020/7/28
 */
@RestRequest(
        path = "/volume-snapshots/shrink/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIShrinkVolumeSnapshotEvent.class,
        isAction = true
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
@MetadataImpact(value = MetadataImpact.Impact.STORAGE, resolver = "SnapshotUuidToVmUuidResolver", field = "uuid")
public class APIShrinkVolumeSnapshotMsg extends APIMessage implements VolumeSnapshotMessage {
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
    public String getVolumeUuid() {
        return volumeUuid;
    }

    @Override
    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    @Override
    public String getTreeUuid() {
        return treeUuid;
    }

    @Override
    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }

    public static APIShrinkVolumeSnapshotMsg __example__() {
        APIShrinkVolumeSnapshotMsg msg = new APIShrinkVolumeSnapshotMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

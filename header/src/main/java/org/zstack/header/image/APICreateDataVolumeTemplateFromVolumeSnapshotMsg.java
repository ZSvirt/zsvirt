package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.tag.TagResourceType;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 */
@RestRequest(
        path = "/images/data-volume-templates/from/volume-snapshots/{snapshotUuid}",
        responseClass = APICreateDataVolumeTemplateFromVolumeSnapshotEvent.class,
        method = HttpMethod.POST,
        parameterName = "params"
)
@TagResourceType(ImageVO.class)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APICreateDataVolumeTemplateFromVolumeSnapshotMsg extends APICreateMessage implements APIAuditor, CreateDataVolumeTemplateMessage {
    @APIParam(resourceType = VolumeSnapshotVO.class)
    private String snapshotUuid;
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(nonempty = true, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
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

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }
 
    public static APICreateDataVolumeTemplateFromVolumeSnapshotMsg __example__() {
        APICreateDataVolumeTemplateFromVolumeSnapshotMsg msg = new APICreateDataVolumeTemplateFromVolumeSnapshotMsg();

        msg.setName("My Data Volume Template");
        msg.setBackupStorageUuids(Collections.singletonList(uuid()));
        msg.setSnapshotUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateDataVolumeTemplateFromVolumeSnapshotEvent)rsp).getInventory().getUuid() : "", ImageVO.class);
    }
}

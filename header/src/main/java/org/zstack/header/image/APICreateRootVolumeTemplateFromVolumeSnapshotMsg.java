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
        path = "/images/root-volume-templates/from/volume-snapshots/{snapshotUuid}",
        responseClass = APICreateRootVolumeTemplateFromVolumeSnapshotEvent.class,
        method = HttpMethod.POST,
        parameterName = "params"
)
@TagResourceType(ImageVO.class)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APICreateRootVolumeTemplateFromVolumeSnapshotMsg extends APICreateMessage implements APIAuditor, CreateRootVolumeTemplateMessage {
    @APIParam(resourceType = VolumeSnapshotVO.class)
    private String snapshotUuid;
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    private String guestOsType;
    @APIParam(nonempty = true, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;
    @APIParam(required = false, validValues = {"Linux", "Windows", "Other", "Paravirtualization", "WindowsVirtio"})
    private String platform;
    private String architecture;
    private boolean system;
    private boolean virtio = true;

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    @Override
    public String getPlatform() {
        return platform;
    }

    @Override
    public void setPlatform(String platform) {
        this.platform = platform;
    }

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

    @Override
    public String getMediaType() {
        return ImageConstant.ImageMediaType.RootVolumeTemplate.toString();
    }

    @Override
    public String getGuestOsType() {
        return guestOsType;
    }

    @Override
    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    @Override
    public boolean isVirtio() {
        return virtio;
    }

    public void setVirtio(boolean virtio) {
        this.virtio = virtio;
    }

    public String getArchitecture() {
        return architecture;
    }

    @Override
    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    @Override
    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }
 
    public static APICreateRootVolumeTemplateFromVolumeSnapshotMsg __example__() {
        APICreateRootVolumeTemplateFromVolumeSnapshotMsg msg = new APICreateRootVolumeTemplateFromVolumeSnapshotMsg();

        msg.setName("My Root Volume Template");
        msg.setBackupStorageUuids(Collections.singletonList(uuid()));
        msg.setSnapshotUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateRootVolumeTemplateFromVolumeSnapshotEvent)rsp).getInventory().getUuid() : "", ImageVO.class);
    }
}

package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;

import java.util.concurrent.TimeUnit;

@TagResourceType(VolumeVO.class)
@RestRequest(
        path = "/volumes/data-volume/from/volume-template/{backupUuid}",
        method = HttpMethod.POST,
        responseClass = APICreateDataVolumeFromVolumeBackupEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APICreateDataVolumeFromVolumeBackupMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(resourceType = VolumeBackupVO.class)
    private String backupUuid;

    @APIParam(required = false, resourceType = BackupStorageVO.class)
    private String backupStorageUuid;

    @APIParam(required = false, resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APINoSee()
    private VolumeBackupVO volumeBackupVO;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getBackupUuid() {
        return backupUuid;
    }

    public void setBackupUuid(String backupUuid) {
        this.backupUuid = backupUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public VolumeBackupVO getVolumeBackupVO() {
        return volumeBackupVO;
    }

    public void setVolumeBackupVO(VolumeBackupVO volumeBackupVO) {
        this.volumeBackupVO = volumeBackupVO;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static APICreateDataVolumeFromVolumeBackupMsg __example__() {
        APICreateDataVolumeFromVolumeBackupMsg msg = new APICreateDataVolumeFromVolumeBackupMsg();

        msg.setBackupUuid(uuid());
        msg.setName("vm1");
        msg.setVmInstanceUuid(uuid());
        msg.setBackupUuid(uuid());
        msg.setBackupStorageUuid(uuid());
        msg.setPrimaryStorageUuid(uuid());

        return msg;
    }
}

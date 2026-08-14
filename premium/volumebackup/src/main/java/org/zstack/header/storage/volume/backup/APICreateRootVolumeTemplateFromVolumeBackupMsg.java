package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.VolumeBackupVO;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/images/root-volume-templates/from/volume-template/{backupUuid}",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateRootVolumeTemplateFromVolumeBackupEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APICreateRootVolumeTemplateFromVolumeBackupMsg extends APICreateMessage implements CreateVolumeTemplateFromBackupMessage, APIAuditor {
    @APIParam(resourceType = VolumeBackupVO.class)
    private String backupUuid;
    @APIParam(resourceType = BackupStorageVO.class)
    private String backupStorageUuid;
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    private String guestOsType;
    @APIParam(required = false, validValues = {"Linux", "Windows", "Other", "Paravirtualization", "WindowsVirtio"})
    private String platform;
    @APIParam(required = false, validValues = {"x86_64", "aarch64", "mips64el", "loongarch64"})
    private String architecture;
    private boolean system;
    private Boolean virtio;

    @Override
    public String getBackupUuid() {
        return backupUuid;
    }

    public void setBackupUuid(String backupUuid) {
        this.backupUuid = backupUuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    @Override
    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    @Override
    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public Boolean getVirtio() {
        return virtio;
    }

    public void setVirtio(Boolean virtio) {
        this.virtio = virtio;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateRootVolumeTemplateFromVolumeBackupEvent)rsp).getInventory().getUuid() : "", ImageVO.class);
    }

    public static APICreateRootVolumeTemplateFromVolumeBackupMsg __example__() {
        APICreateRootVolumeTemplateFromVolumeBackupMsg msg = new APICreateRootVolumeTemplateFromVolumeBackupMsg();

        msg.setName("template");
        msg.setDescription("root template from volume backup");
        msg.setBackupStorageUuid(uuid());
        msg.setBackupUuid(uuid());

        return msg;
    }
}

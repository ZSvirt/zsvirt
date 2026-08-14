package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.TagResourceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@TagResourceType(ImageVO.class)
@RestRequest(
        path = "/images",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddImageEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APIAddImageMsg extends APICreateMessage implements APIAuditor, AddImageMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(maxLength = 1024)
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;
    @APIParam(required = false, validValues = {"RootVolumeTemplate", "ISO", "DataVolumeTemplate"})
    private String mediaType;
    @APIParam(maxLength = 255, required = false)
    private String guestOsType;
    @APIParam(required = false, maxLength = 32, validValues = {"x86_64", "aarch64", "mips64el", "loongarch64"})
    private String architecture;
    private boolean system;
    @APIParam(required = false)
    private String format;
    @APIParam(required = false, validValues = {"Linux", "Windows", "Other", "Paravirtualization", "WindowsVirtio"})
    private String platform;
    @APIParam(nonempty = true, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;
    private String type;
    @APIParam(required = false)
    private boolean virtio = true;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public List<String> getBackupStorageUuids() {
        if (backupStorageUuids == null) {
            backupStorageUuids = new ArrayList<String>();
        }
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String type) {
        this.mediaType = type;
    }

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getType() {
        return type;
    }

    public void setType(String imageType) {
        this.type = imageType;
    }

    public boolean isVirtio() {
        return virtio;
    }

    public void setVirtio(boolean virtio) {
        this.virtio = virtio;
    }
 
    public static APIAddImageMsg __example__() {
        APIAddImageMsg msg = new APIAddImageMsg();

        msg.setName("TinyLinux");
        msg.setBackupStorageUuids(Collections.singletonList(uuid()));
        msg.setUrl("http://192.168.1.20/share/images/tinylinux.qcow2");
        msg.setFormat(ImageConstant.QCOW2_FORMAT_STRING);
        msg.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());
        msg.setPlatform(ImagePlatform.Linux.toString());
        msg.setArchitecture(ImageArchitecture.x86_64.toString());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddImageEvent)rsp).getInventory().getUuid() : "", ImageVO.class);
    }
}

package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/14/2015.
 */
@RestRequest(
        path = "/images/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateImageEvent.class,
        isAction = true
)
public class APIUpdateImageMsg extends APIMessage implements ImageMessage {
    @APIParam(resourceType = ImageVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(maxLength = 255, required = false)
    private String guestOsType;
    @APIParam(maxLength = 255, validValues = {"RootVolumeTemplate", "DataVolumeTemplate", "ISO"}, required = false)
    private String mediaType;
    @APIParam(maxLength = 255, validValues = {"raw", "qcow2", "iso"}, required = false)
    private String format;
    private Boolean system;
    @APIParam(required = false, validValues = {"Linux", "Windows", "Other", "Paravirtualization", "WindowsVirtio"})
    private String platform;
    @APIParam(required = false, maxLength = 32, validValues = {"x86_64", "aarch64", "mips64el", "loongarch64"})
    private String architecture;
    @APIParam(required = false)
    private Boolean virtio;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public Boolean getSystem() {
        return system;
    }

    public void setSystem(Boolean system) {
        this.system = system;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public Boolean getVirtio() {
        return virtio;
    }

    public void setVirtio(Boolean virtio) {
        this.virtio = virtio;
    }

    @Override
    public String getImageUuid() {
        return uuid;
    }
 
    public static APIUpdateImageMsg __example__() {
        APIUpdateImageMsg msg = new APIUpdateImageMsg();

        msg.setUuid(uuid());
        msg.setPlatform(ImagePlatform.Windows.toString());

        return msg;
    }
}

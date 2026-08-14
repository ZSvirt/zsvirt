package org.zstack.header.image;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2020/7/7.
 */
public class UpdateImageMsg extends NeedReplyMessage implements ImageMessage {
    private String uuid;
    private String name;
    private String description;
    private String guestOsType;
    private String mediaType;
    private String format;
    private Boolean system;
    private String platform;
    private String architecture;
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

    public static UpdateImageMsg valueOf(APIUpdateImageMsg msg) {
        UpdateImageMsg result = new UpdateImageMsg();

        result.uuid = msg.getUuid();
        result.name = msg.getName();
        result.description = msg.getDescription();
        result.guestOsType = msg.getGuestOsType();
        result.mediaType = msg.getMediaType();
        result.format = msg.getFormat();
        result.system = msg.getSystem();
        result.platform = msg.getPlatform();
        result.architecture = msg.getArchitecture();
        result.virtio = msg.getVirtio();
        return result;
    }
}

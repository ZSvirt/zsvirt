package org.zstack.header.image;

import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ShadowEntity;

import javax.persistence.*;
import java.sql.Timestamp;

@MappedSuperclass
public class ImageAO extends ResourceVO implements ShadowEntity {
    @Column
    @Index
    private String name;

    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private ImageStatus status;

    @Column
    @Enumerated(EnumType.STRING)
    private ImageState state;

    @Column
    // size indicate the image file disk size
    private long size;

    @Column
    // actualSize indicate the OS FileSystem disk size
    private long actualSize;

    @Column
    private String md5Sum;

    @Column
    @Enumerated(EnumType.STRING)
    private ImagePlatform platform;

    @Column
    private String type;

    @Column
    private String format;

    @Column
    private String url;

    @Column
    private Boolean system;

    @Column
    @Enumerated(EnumType.STRING)
    private ImageMediaType mediaType;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Column
    private String guestOsType;

    @Column
    private String architecture;

    @Transient
    private ImageAO shadow;

    @Column
    private Boolean virtio;

    public ImageAO getShadow() {
        return shadow;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public long getActualSize() {
        return actualSize;
    }

    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }

    public boolean isSystem() {
        return system == null ? false : system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public ImagePlatform getPlatform() {
        return platform;
    }

    public void setPlatform(ImagePlatform platform) {
        this.platform = platform;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
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

    public ImageState getState() {
        return state;
    }

    public void setState(ImageState state) {
        this.state = state;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMd5Sum() {
        return md5Sum;
    }

    public void setMd5Sum(String md5Sum) {
        this.md5Sum = md5Sum;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ImageMediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(ImageMediaType type) {
        this.mediaType = type;
    }

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public ImageStatus getStatus() {
        return status;
    }

    public void setStatus(ImageStatus status) {
        this.status = status;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public void setShadow(Object o) {
        shadow = (ImageAO) o;
    }


    public Boolean getVirtio() {
        return virtio;
    }

    public void setVirtio(Boolean virtio) {
        this.virtio = virtio;
    }

}

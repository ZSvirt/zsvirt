package org.zstack.header.storage.primary;

import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class ImageCacheShadowVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = PrimaryStorageEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String primaryStorageUuid;

    @Column
    private String imageUuid;

    @Column
    private String installUrl;

    @Column
    @Enumerated(EnumType.STRING)
    private ImageMediaType mediaType;

    @Column
    private long size;

    @Column
    @Enumerated(EnumType.STRING)
    private ImageCacheState state = ImageCacheState.ready;

    @Column
    private String md5sum;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public ImageCacheShadowVO(ImageCacheVO c) {
        primaryStorageUuid = c.getPrimaryStorageUuid();
        imageUuid = c.getImageUuid();
        installUrl = c.getInstallUrl();
        mediaType = c.getMediaType();
        size = c.getSize();
        state = c.getState();
        md5sum = c.getMd5sum();
        createDate = c.getCreateDate();
        lastOpDate = c.getLastOpDate();
    }

    public ImageCacheShadowVO() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getInstallUrl() {
        return installUrl;
    }

    public void setInstallUrl(String installUrl) {
        this.installUrl = installUrl;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
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

    public ImageMediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(ImageMediaType type) {
        this.mediaType = type;
    }

    public ImageCacheState getState() {
        return state;
    }

    public void setState(ImageCacheState state) {
        this.state = state;
    }
}

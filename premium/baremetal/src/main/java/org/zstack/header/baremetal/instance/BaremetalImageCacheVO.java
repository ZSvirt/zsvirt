package org.zstack.header.baremetal.instance;

import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.ShadowEntity;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 7/20/18.
 */

@Entity
@Table
@BaseResource
public class BaremetalImageCacheVO implements ToInventory, ShadowEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = BaremetalPxeServerVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String pxeServerUuid;

    @Column
    private String imageUuid;

    @Column
    private String url;

    @Column
    private String installUrl;

    @Column
    @Enumerated(EnumType.STRING)
    private ImageConstant.ImageMediaType mediaType;

    @Column
    private Long size;

    @Column
    private Long actualSize;

    @Column
    private String md5sum;

    @Column
    private Long utilization;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @Transient
    private BaremetalImageCacheVO shadow;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPxeServerUuid() {
        return pxeServerUuid;
    }

    public void setPxeServerUuid(String pxeServerUuid) {
        this.pxeServerUuid = pxeServerUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInstallUrl() {
        return installUrl;
    }

    public void setInstallUrl(String installUrl) {
        this.installUrl = installUrl;
    }

    public ImageConstant.ImageMediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(ImageConstant.ImageMediaType mediaType) {
        this.mediaType = mediaType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getActualSize() {
        return actualSize;
    }

    public void setActualSize(Long actualSize) {
        this.actualSize = actualSize;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public Long getUtilization() {
        return utilization;
    }

    public void setUtilization(Long utilization) {
        this.utilization = utilization;
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

    public BaremetalImageCacheVO getShadow() {
        return shadow;
    }

    public void setShadow(BaremetalImageCacheVO shadow) {
        this.shadow = shadow;
    }

    @Override
    public void setShadow(Object o) {
        shadow = (BaremetalImageCacheVO) o;
    }
}

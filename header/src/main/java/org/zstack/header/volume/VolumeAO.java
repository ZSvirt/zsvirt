package org.zstack.header.volume;

import org.zstack.header.configuration.DiskOfferingEO;
import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ShadowEntity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

@MappedSuperclass
public class VolumeAO extends ResourceVO implements ShadowEntity {
    @Column
    @Index
    private String name;

    @Column
    private String description;

    @Column
    @ForeignKey(parentEntityClass = PrimaryStorageEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String primaryStorageUuid;

    @Column
    private String vmInstanceUuid;

    @Column
    @ForeignKey(parentEntityClass = DiskOfferingEO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String diskOfferingUuid;

    @Column
    private String rootImageUuid;

    @Column
    private String installPath;

    @Column
    @Enumerated(EnumType.STRING)
    private VolumeType type;

    @Column
    @Enumerated(EnumType.STRING)
    private VolumeStatus status;

    @Column
    private long size;

    @Column
    private Long actualSize;

    @Column
    private Integer deviceId;

    @Column
    private String format;

    @Column
    @Enumerated(EnumType.STRING)
    private VolumeState state;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Column
    private String lastVmInstanceUuid;

    @Column
    private Timestamp lastDetachDate;

    @Column
    private Timestamp lastAttachDate;

    @Column
    private boolean isShareable;

    @Column
    private String volumeQos;

    @Column
    private String protocol;

    @Transient
    private VolumeAO shadow;

    public VolumeAO() {
        this.state = VolumeState.Enabled;
    }

    public boolean isShareable() {
        return isShareable;
    }

    public void setShareable(boolean shareable) {
        isShareable = shareable;
    }

    public VolumeAO getShadow() {
        return shadow;
    }

    @Override
    public void setShadow(Object o) {
        shadow = (VolumeAO) o;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Long getActualSize() {
        return actualSize;
    }

    public void setActualSize(Long actualSize) {
        this.actualSize = actualSize;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDiskOfferingUuid() {
        return diskOfferingUuid;
    }

    public void setDiskOfferingUuid(String diskOfferingUuid) {
        this.diskOfferingUuid = diskOfferingUuid;
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

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public VolumeType getType() {
        return type;
    }

    public void setType(VolumeType volumeType) {
        this.type = volumeType;
    }

    public boolean isDisk() {
        return type == VolumeType.Data || type == VolumeType.Root;
    }

    public boolean isDataVolume() {
        return type == VolumeType.Data;
    }

    public boolean isMemoryVolume() {
        return type == VolumeType.Memory;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public VolumeState getState() {
        return state;
    }

    public void setState(VolumeState state) {
        this.state = state;
    }

    public boolean isAttached() {
        return this.vmInstanceUuid != null;
    }

    public String getRootImageUuid() {
        return rootImageUuid;
    }

    public void setRootImageUuid(String rootImageUuid) {
        this.rootImageUuid = rootImageUuid;
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

    public String getLastVmInstanceUuid() {
        return lastVmInstanceUuid;
    }

    public void setLastVmInstanceUuid(String lastVmInstanceUuid) {
        this.lastVmInstanceUuid = lastVmInstanceUuid;
    }

    public Timestamp getLastDetachDate() {
        return lastDetachDate;
    }

    public void setLastDetachDate(Timestamp lastDetachDate) {
        this.lastDetachDate = lastDetachDate;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public VolumeStatus getStatus() {
        return status;
    }

    public void setStatus(VolumeStatus status) {
        this.status = status;
    }

    public String getVolumeQos() {
        return volumeQos;
    }

    public void setVolumeQos(String volumeQos) {
        this.volumeQos = volumeQos;
    }

    public Timestamp getLastAttachDate() {
        return lastAttachDate;
    }

    public void setLastAttachDate(Timestamp lastAttachDate) {
        this.lastAttachDate = lastAttachDate;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}

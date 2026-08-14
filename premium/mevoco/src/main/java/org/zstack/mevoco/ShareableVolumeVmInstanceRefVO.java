package org.zstack.mevoco;

import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.volume.VolumeEO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class ShareableVolumeVmInstanceRefVO {
    @Id
    @Column
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = VolumeEO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String volumeUuid;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    private Integer deviceId;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
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

}

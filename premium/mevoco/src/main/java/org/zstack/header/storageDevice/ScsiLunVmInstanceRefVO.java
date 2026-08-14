package org.zstack.header.storageDevice;

import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/1
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = ScsiLunVO.class, joinColumn = "scsiLunUuid"),
        @SoftDeletionCascade(parent = VmInstanceVO.class, joinColumn = "vmInstanceUuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = ScsiLunVO.class, myField = "scsiLunUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmInstanceUuid", targetField = "uuid"),
        }
)
public class ScsiLunVmInstanceRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    @ForeignKey(parentEntityClass = ScsiLunVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String scsiLunUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Column
    private Integer deviceId;

    @Column
    private boolean attachMultipath;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public ScsiLunVmInstanceRefVO() {
    }

    public String getScsiLunUuid() {
        return scsiLunUuid;
    }

    public void setScsiLunUuid(String scsiLunUuid) {
        this.scsiLunUuid = scsiLunUuid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isAttachMultipath() {
        return attachMultipath;
    }

    public void setAttachMultipath(boolean attachMultipath) {
        this.attachMultipath = attachMultipath;
    }
}

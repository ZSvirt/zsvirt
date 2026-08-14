package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.*;
import org.zstack.mttyDevice.MttyDeviceVO;
import org.zstack.pciDevice.PciDeviceVO;
import org.zstack.pciDevice.specification.mdev.MdevDeviceSpecVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2019-04-17.
 */
@Entity
@Table
@BaseResource
@AutoDeleteTag
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = HostEO.class, joinColumn = "hostUuid"),
        @SoftDeletionCascade(parent = VmInstanceEO.class, joinColumn = "vmInstanceUuid")
})
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = PciDeviceVO.class, myField = "parentUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = MttyDeviceVO.class, myField = "mttyUuid", targetField = "uuid"),
        },

        friends = {
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmInstanceUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = MdevDeviceSpecVO.class, myField = "mdevSpecUuid", targetField = "uuid"),
        }
)
public class MdevDeviceVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    private String name;

    @Column
    private String description;

    @Index
    @Column
    @ForeignKey(parentEntityClass = PciDeviceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String parentUuid;

    @Index
    @Column
    @ForeignKey(parentEntityClass = MttyDeviceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String mttyUuid;

    @Index
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String vmInstanceUuid;

    @Index
    @Column
    @ForeignKey(parentEntityClass = MdevDeviceSpecVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String mdevSpecUuid;

    @Index
    @Column
    @Enumerated(EnumType.STRING)
    private MdevDeviceType type;

    @Column
    @Enumerated(EnumType.STRING)
    private MdevDeviceState state;

    @Column
    @Enumerated(EnumType.STRING)
    private MdevDeviceStatus status;
    
    // spec or device
    @Column
    @Enumerated(EnumType.STRING)
    private MdevDeviceChooser chooser;

    @Column
    private String vendor;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @PreUpdate
    void preUpdate() {
        lastOpDate = null;
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

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getMttyUuid() {
        return mttyUuid;
    }

    public void setMttyUuid(String mttyUuid) {
        this.mttyUuid = mttyUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getMdevSpecUuid() {
        return mdevSpecUuid;
    }

    public void setMdevSpecUuid(String mdevSpecUuid) {
        this.mdevSpecUuid = mdevSpecUuid;
    }

    public MdevDeviceType getType() {
        return type;
    }

    public void setType(MdevDeviceType type) {
        this.type = type;
    }

    public MdevDeviceState getState() {
        return state;
    }

    public void setState(MdevDeviceState state) {
        this.state = state;
    }

    public MdevDeviceStatus getStatus() {
        return status;
    }

    public void setStatus(MdevDeviceStatus status) {
        this.status = status;
    }

    public MdevDeviceChooser getChooser() {
        return chooser;
    }

    public void setChooser(MdevDeviceChooser chooser) {
        this.chooser = chooser;
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

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
}

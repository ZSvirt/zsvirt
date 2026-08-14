package org.zstack.header.vm.cdrom;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.image.ImageEO;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;
import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Create by lining at 2018/12/26
 */
@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmInstanceUuid", targetField = "uuid")
        }
)
public class VmCdRomVO extends ResourceVO implements OwnedByAccount {
    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    private Integer deviceId;

    /**
     * The occupant of the cdrom.
     * Maybe 'ISO' for cdrom iso, or 'GuestTools' for guest-tools, or null for empty cdrom.
     *
     * @see org.zstack.header.vm.VmInstanceConstant#VM_CDROM_OCCUPANT_ISO
     * @see org.zstack.header.vm.VmInstanceConstant#VM_CDROM_OCCUPANT_GUEST_TOOLS
     */
    @Column
    private String occupant;

    @Column
    @ForeignKey(parentEntityClass = ImageEO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String isoUuid;

    @Column
    @Index
    private String isoInstallPath;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String protocol;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public String getOccupant() {
        return occupant;
    }

    public void setOccupant(String occupant) {
        this.occupant = occupant;
    }

    public String getIsoUuid() {
        return isoUuid;
    }

    public void setIsoUuid(String isoUuid) {
        this.isoUuid = isoUuid;
    }

    public String getIsoInstallPath() {
        return isoInstallPath;
    }

    public void setIsoInstallPath(String isoInstallPath) {
        this.isoInstallPath = isoInstallPath;
        if (isoInstallPath == null) {
            this.protocol = null;
        }
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}

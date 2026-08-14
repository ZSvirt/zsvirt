package org.zstack.header.sriov;

import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;
import org.zstack.pciDevice.PciDeviceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * Created by GuoYi on 11/28/19.
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class VmVfNicVO extends VmNicVO {
    @Column
    @Index
    @ForeignKey(parentEntityClass = PciDeviceVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String pciDeviceUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private VmVfNicHaState haState = VmVfNicHaState.Disabled;

    public VmVfNicVO() {
    }

    public VmVfNicVO(VmNicVO nic) {
        this.setUuid(nic.getUuid());
        this.setIp(nic.getIp());
        this.setL3NetworkUuid(nic.getL3NetworkUuid());
        this.setUsedIpUuid(nic.getUsedIpUuid());
        this.setVmInstanceUuid(nic.getVmInstanceUuid());
        this.setDeviceId(nic.getDeviceId());
        this.setMac(nic.getMac());
        this.setHypervisorType(nic.getHypervisorType());
        this.setNetmask(nic.getNetmask());
        this.setGateway(nic.getGateway());
        this.setIpVersion(nic.getIpVersion());
        this.setInternalName(nic.getInternalName());
        this.setDriverType(nic.getDriverType());
        this.setAccountUuid(nic.getAccountUuid());
        this.setMetaData(nic.getMetaData());
    }

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    public VmVfNicHaState getHaState() {
        return haState;
    }

    public void setHaState(VmVfNicHaState haState) {
        this.haState = haState;
    }
}

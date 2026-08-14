package org.zstack.header.vdpa;

import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;
import org.zstack.pciDevice.PciDeviceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
public class VmVdpaNicVO extends VmNicVO {
    @Column
    @Index
    @ForeignKey(parentEntityClass = PciDeviceVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String pciDeviceUuid;

    @Column
    private String lastPciDeviceUuid;

    @Column
    private String srcPath;

    public VmVdpaNicVO() {
    }

    public VmVdpaNicVO(VmNicVO nic) {
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
        this.setAccountUuid(nic.getAccountUuid());
        this.setDriverType(nic.getDriverType());
        this.setMetaData(nic.getMetaData());
    }

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    public String getLastPciDeviceUuid() {
        return lastPciDeviceUuid;
    }

    public void setLastPciDeviceUuid(String lastPciDeviceUuid) {
        this.lastPciDeviceUuid = lastPciDeviceUuid;
    }

    public String getSrcPath() {
        return srcPath;
    }

    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    }
}

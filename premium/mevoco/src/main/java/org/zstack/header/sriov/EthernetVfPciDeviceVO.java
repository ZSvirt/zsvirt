package org.zstack.header.sriov;

import org.zstack.header.host.HostEO;
import org.zstack.header.network.l3.L3NetworkEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;
import org.zstack.pciDevice.*;

import javax.persistence.*;

/**
 * Created by shixin.ruan 2023/10/20
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class EthernetVfPciDeviceVO extends PciDeviceVO {

    /* 冗余字段: PciDeviceVO.hostUuid，为了方便debug */
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostDevUuid;

    @Column
    private String interfaceName;


    /* 冗余字段: PciDeviceVO.vmInstanceUuid，为了方便debug */
    @Column
    @ForeignKey(parentEntityClass = VmInstanceVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String vmUuid;

    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String l3NetworkUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private EthernetVfStatus vfStatus;


    public EthernetVfPciDeviceVO() {
    }

    public EthernetVfPciDeviceVO(PciDeviceVO device) {
        this.setUuid(device.getUuid());
        this.setName(device.getName());
        this.setDescription(device.getDescription());
        this.setHostUuid(device.getHostUuid());
        this.setParentUuid(device.getParentUuid());
        this.setVmInstanceUuid(device.getVmInstanceUuid());
        this.setPciSpecUuid(device.getPciSpecUuid());
        this.setState(device.getState());
        this.setStatus(device.getStatus());
        this.setVendorId(device.getVendorId());
        this.setDeviceId(device.getDeviceId());
        this.setSubvendorId(device.getSubvendorId());
        this.setSubdeviceId(device.getSubdeviceId());
        this.setPciDeviceAddress(device.getPciDeviceAddress());
        this.setIommuGroup(device.getIommuGroup());
        this.setType(device.getType());
        this.setVirtStatus(device.getVirtStatus());
        this.setPassThroughState(device.getPassThroughState());
        this.setChooser(device.getChooser());
        this.setMetaData(device.getMetaData());
        this.setPciDeviceMetaData(device.getPciDeviceMetaData());
        this.setCreateDate(device.getCreateDate());
        this.setLastOpDate(device.getLastOpDate());
        this.setMdevSpecRefs(device.getMdevSpecRefs());
        this.setAccountUuid(device.getAccountUuid());
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public EthernetVfStatus getVfStatus() {
        return vfStatus;
    }

    public void setVfStatus(EthernetVfStatus vfStatus) {
        this.vfStatus = vfStatus;
    }

    public String getHostDevUuid() {
        return hostDevUuid;
    }

    public void setHostDevUuid(String hostDevUuid) {
        this.hostDevUuid = hostDevUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }
}

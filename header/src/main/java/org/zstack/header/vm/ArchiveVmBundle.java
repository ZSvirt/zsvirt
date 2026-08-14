package org.zstack.header.vm;

/**
 * Created by LiangHanYu on 2022/9/21 15:52
 */
public class ArchiveVmBundle extends ArchiveBundle {
    VmInstanceInventory vmInventory;
    String xml;

    public ArchiveVmBundle() {
    }

    public ArchiveVmBundle(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }

    public ArchiveVmBundle(String xml) {
        this.xml = xml;
    }

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }

    public void setVmInventory(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public UpdateVmInstanceMsg getUpdateVmMessage() {
        if (getVmInventory() == null) {
            throw new IllegalStateException("vmInventory is null; cannot build UpdateVmInstanceMsg");
        }
        UpdateVmInstanceMsg umsg = new UpdateVmInstanceMsg();
        umsg.setUuid(getVmInventory().getUuid());
        umsg.setName(getVmInventory().getName());
        umsg.setDescription(getVmInventory().getDescription());
        umsg.setDefaultL3NetworkUuid(getVmInventory().getDefaultL3NetworkUuid());
        umsg.setCpuNum(getVmInventory().getCpuNum());
        umsg.setMemorySize(getVmInventory().getMemorySize());
        umsg.setPlatform(getVmInventory().getPlatform());
        umsg.setGuestOsType(getVmInventory().getGuestOsType());
        return umsg;
    }
}

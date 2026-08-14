package org.zstack.network.service.userdata;

import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;

import java.util.List;

/**
 * Created by frank on 10/13/2015.
 */
public class UserdataStruct {
    private List<String> userdataList;
    private String l3NetworkUuid;
    private String vmUuid;
    private List<VmNicInventory> vmNics;
    private String hostUuid;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public List<VmNicInventory> getVmNics() {
        return vmNics;
    }

    public void setVmNics(List<VmNicInventory> vmNics) {
        this.vmNics = vmNics;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public void setParametersFromVmSpec(VmInstanceSpec vmSpec) {
        vmUuid = vmSpec.getVmInventory().getUuid();
        vmNics = vmSpec.getDestNics();
        hostUuid = vmSpec.getDestHost().getUuid();
    }

    public void setParametersFromVmInventory(VmInstanceInventory inv) {
        vmUuid = inv.getUuid();
        vmNics = inv.getVmNics();
        hostUuid = inv.getHostUuid();
    }

    public List<String> getUserdataList() {
        return userdataList;
    }

    public void setUserdataList(List<String> userdataList) {
        this.userdataList = userdataList;
    }
}

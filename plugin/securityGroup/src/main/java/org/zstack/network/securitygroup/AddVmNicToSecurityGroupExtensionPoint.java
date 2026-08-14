package org.zstack.network.securitygroup;

import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;

import java.util.List;

public interface AddVmNicToSecurityGroupExtensionPoint {
    void preAddVmNic(SecurityGroupInventory securityGroup, VmInstanceInventory vm, List<VmNicInventory> nics) throws SecurityGroupException;
    
    void beforeAddVmNic(SecurityGroupInventory securityGroup, VmInstanceInventory vm, List<VmNicInventory> nics);
    
    void afterAddVmNic(SecurityGroupInventory securityGroup, VmInstanceInventory vm, List<VmNicInventory> nics);
}

package org.zstack.appliancevm;

import org.zstack.core.cascade.CascadeAction;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;

import java.util.List;

/**
 * Created by weiwang on 22/10/2017
 */
public interface ApvmCascadeFilterExtensionPoint {
    List<ApplianceVmVO> filterApplianceVmCascade(List<ApplianceVmVO> applianceVmVOS, CascadeAction action,
                                                 String parentIssuer, List<String> parentIssuerUuids,
                                                 List<VmNicInventory> toDeleteNics,
                                                 List<UsedIpInventory> toDeleteIps);
}

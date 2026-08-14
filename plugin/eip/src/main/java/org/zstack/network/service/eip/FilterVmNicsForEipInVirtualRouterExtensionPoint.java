package org.zstack.network.service.eip;

import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;

import java.util.List;

/**
 * Created by heathhose on 17-5-13.
 */
public interface FilterVmNicsForEipInVirtualRouterExtensionPoint {
    List<VmNicInventory> filterVmNicsForEipInVirtualRouter(VipInventory vip, List<VmNicInventory> vmNics);

}

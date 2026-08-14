package org.zstack.network.service.eip;

import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.VmNicInventory;

import java.util.HashMap;
import java.util.List;


public interface GetEipAttachableL3UuidsForVmNicExtensionPoint {
    HashMap<Boolean, List<String>> getEipAttachableL3UuidsForVmNic(VmNicInventory vmNicInv, L3NetworkVO l3Network);
}


package org.zstack.compute.vm;

import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicType;

import java.util.List;

public interface VmNicManager {

    List<String> getSupportNicDriverTypes();

    String getDefaultPVNicDriver();

    String getDefaultNicDriver();

    String getPcNetNicDriver();

    void setNicDriverType(VmNicInventory nic, boolean isImageSupportVirtIo, boolean isParaVirtualization, VmInstanceInventory vm);

    VmNicType getVmNicType(String vmUuid, L3NetworkInventory l3nw, boolean enableSriov);
}

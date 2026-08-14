package org.zstack.kvm;

import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.network.l2.VSwitchType;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMAgentCommands.NicTO;

public interface KVMCompleteNicInformationExtensionPoint {
	NicTO completeNicInformation(L2NetworkInventory l2Network, L3NetworkInventory l3Network, VmNicInventory nic);
	String getBridgeName(L2NetworkInventory l2Network);
	L2NetworkType getL2NetworkTypeVmNicOn();
}

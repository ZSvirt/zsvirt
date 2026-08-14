package org.zstack.network.service.virtualrouter;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;

public interface VirtualRouterHypervisorBackend {
	HypervisorType getVirtualRouterSupportedHypervisorType();
	
	void createVirtualRouterBootstrapIso(VirtualRouterBootstrapIsoInventory iso, VmInstanceSpec vrSpec, Completion complete);
	
	void deleteVirtualRouterBootstrapIso(VirtualRouterBootstrapIsoInventory iso, VmInstanceInventory vr, Completion complete);
}

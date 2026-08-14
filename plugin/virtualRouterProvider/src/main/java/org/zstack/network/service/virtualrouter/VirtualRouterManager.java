package org.zstack.network.service.virtualrouter;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmNicInventory;

import java.util.List;

public interface VirtualRouterManager {

    VirtualRouterHypervisorBackend getHypervisorBackend(HypervisorType hypervisorType);
	
    String buildUrl(String mgmtNicIp, String subPath);

    List<String> selectL3NetworksNeedingSpecificNetworkService(List<String> candidate, NetworkServiceType nsType);

    List<String> selectGuestL3NetworksNeedingSpecificNetworkService(List<String> candidate, NetworkServiceType nsType, String publicUuid);

    boolean isL3NetworkNeedingNetworkServiceByVirtualRouter(String l3Uuid, String nsType);

    boolean isL3NetworksNeedingNetworkServiceByVirtualRouter(List<String> l3Uuids, String nsType);

    void acquireVirtualRouterVm(VirtualRouterStruct struct, ReturnValueCompletion<VirtualRouterVmInventory> completion);

    VirtualRouterVmInventory getVirtualRouterVm(L3NetworkInventory l3Nw);

    boolean isVirtualRouterRunningForL3Network(String l3Uuid);

    long countVirtualRouterRunningForL3Network(String l3Uuid);

    boolean isVirtualRouterForL3Network(String l3Uuid);

    List<Flow> getPostCreateFlows();

    List<Flow> getPostStartFlows();

    List<Flow> getPostRebootFlows();

    List<Flow> getPostStopFlows();

    List<Flow> getPostMigrateFlows();

    List<Flow> getPostDestroyFlows();

    FlowChain getReconnectFlowChain();

    FlowChain getProvisionConfigChain();

    int getParallelismDegree(String vrUuid);

    VmNicInventory getSnatPubicInventory(VirtualRouterVmInventory vrInv);

    VmNicInventory getSnatPubicInventory(VirtualRouterVmInventory vrInv, String L3NetworkUuid);

    void changeVirutalRouterDefaultL3Network(String vrUuid, String newL3Uuid, String oldL3Uuid, Completion completion);

    List<String> getVirtualRouterVips(String vrUuid, List<String> vipUuids);

    List<String> getPublicL3UuidsOfPrivateL3(L3NetworkVO privateL3);
}

package org.zstack.network.service.virtualrouter.ha;

import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.OverlayMessage;
import org.zstack.header.network.service.VirtualRouterHaCallbackInterface;
import org.zstack.header.network.service.VirtualRouterHaTask;
import org.zstack.header.vm.VmInstanceInventory;

public interface VirtualRouterHaBackend {
    NoRollbackFlow getAttachL3NetworkFlow();
    void detachL3NetworkFromVirtualRouterHaGroup(String vrUuid, String l3NetworkUuid, boolean isRollback, Completion completion);
    void submitVirtualRouterHaTask(VirtualRouterHaTask task, Completion completion);
    boolean isSnatEnabledOnHaRouter(String vrUuid);
    void cleanupHaNetworkService(VmInstanceInventory vrInv, Completion completion);
    String getVirtualRouterHaName(String vrUuid);
    String getVirtualRouterHaUuid(String vrUuid);
    VirtualRouterHaCallbackInterface getCallback(String type);
    String getVirtualRouterPeerUuid(String vrUuid);
    void virtualRouterOverlayMsgHandle(OverlayMessage msg, Completion completion);
}

package org.zstack.compute.vm;

import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.message.Message;
import org.zstack.header.vm.*;

public interface VmInstanceManager {
    FlowChain getCreateVmWorkFlowChain(VmInstanceInventory inv);
    
    FlowChain getStopVmWorkFlowChain(VmInstanceInventory inv);
    
    FlowChain getRebootVmWorkFlowChain(VmInstanceInventory inv);
    
    FlowChain getStartVmWorkFlowChain(VmInstanceInventory inv);
    
    FlowChain getDestroyVmWorkFlowChain(VmInstanceInventory inv);
    
    FlowChain getMigrateVmWorkFlowChain(VmInstanceInventory inv);
    
    FlowChain getAttachUninstantiatedVolumeWorkFlowChain(VmInstanceInventory inv);

    FlowChain getAttachIsoWorkFlowChain(VmInstanceInventory inv);

    FlowChain getDetachIsoWorkFlowChain(VmInstanceInventory inv);

    FlowChain getExpungeVmWorkFlowChain(VmInstanceInventory inv);

    FlowChain getPauseWorkFlowChain(VmInstanceInventory inv);

    FlowChain getResumeVmWorkFlowChain(VmInstanceInventory inv);

    VmInstanceFactory getVmInstanceFactory(VmInstanceType vmType);

    VmInstanceBaseExtensionFactory getVmInstanceBaseExtensionFactory(Message msg);

    VmInstanceNicFactory getVmInstanceNicFactory(VmNicType type);

    VmNicQosConfigBackend getVmNicQosConfigBackend(String type);

    VmDnsBackend getVmDnsBackend(String type);
}

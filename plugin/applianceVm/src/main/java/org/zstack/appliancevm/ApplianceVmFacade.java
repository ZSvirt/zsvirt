package org.zstack.appliancevm;

import org.zstack.header.core.workflow.Flow;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:42 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ApplianceVmFacade {
    void createApplianceVm(ApplianceVmSpec spec, ReturnValueCompletion<ApplianceVmInventory> completion);

    void startApplianceVm(String vmUuid, ReturnValueCompletion<ApplianceVmInventory> completion);

    void stopApplianceVm(String vmUuid, ReturnValueCompletion<ApplianceVmInventory> completion);

    void rebootApplianceVm(String vmUuid, ReturnValueCompletion<ApplianceVmInventory> completion);

    void destroyApplianceVm(String vmUuid, ReturnValueCompletion<ApplianceVmInventory> completion);

    void destroyApplianceVm(String vmUuid);

    FlowChainBuilder getCreateApplianceVmWorkFlowBuilder();

    Map<String, Object> prepareBootstrapInformation(VmInstanceSpec spec);

    Flow createBootstrapFlow(HypervisorType hvType);

    void openFirewall(String applianceVmUuid, String l3uuid, List<ApplianceVmFirewallRuleInventory> rules, Completion completion);

    void removeFirewall(String applianceVmUuid, String l3uuid, List<ApplianceVmFirewallRuleInventory> rules, Completion completion);

    void attachApplianceVmToAffinityGroup(String vmUuid, String affinityGroupUuid);
    void attachApplianceVmToHaGroup(String vmUuid, String haGroupUuid);
    void dettachVmInstanceFromAffinityGroup(String vmUuid);
    void detachVirtualRouterFromHaGroup(String vmUuid, String haGroupUuid);
}

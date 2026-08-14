package org.zstack.guesttools;

import org.zstack.guesttools.kvm.GuestToolsKvmCommands;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.Map;
import java.util.Set;

/**
 * Created by GuoYi on 2019-09-18.
 */
public interface GuestToolsHypervisorBackend {
    GuestToolsAgentType getGuestToolsAgentType();
    String getSrcGuestToolsIso(String archType, String HypervisorType, String version);
    String getDstGuestToolsIso();

    void downloadGuestToolsIsoToHost(HostInventory host, String version, Completion completion);
    void attachGuestToolsIsoToVm(VmInstanceInventory vm, Completion completion);
    void detachGuestToolsIsoFromVm(VmInstanceInventory vm, Completion completion);
    void getVmGuestToolsInfo(VmInstanceInventory vm, boolean readMetricOnly, ReturnValueCompletion<GuestToolsKvmCommands.GetVmGuestToolsInfoRsp> completion);
    void getVmMetricsRoutingStatus(VmInstanceInventory vm, Set<String> checkItems, ReturnValueCompletion<Map<String, String>> completion);
    void updateHostGuestToolsTag(String hostUuid, String version);
    String getHostGuestToolsTag(String hostUuid);

    void checkGuestToolsInfoBeforeVmStop(VmInstanceInventory inv, String version);
    void beforeVmMigrate(VmInstanceInventory vm, Completion completion);
}

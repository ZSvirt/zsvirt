package org.zstack.ha;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.List;

public interface VmFencerRuleExtensionPoint {
    AddVmFencerRuleToHostMsg generateAddVmFencerRuleToHostMsg(List<String> vmUuids, String hostUuid);

    RemoveVmFencerRuleFromHostMsg generateVmFencerRuleFromHostMsg(VmInstanceInventory inv, String hostUuid);

    void sendVmFencerRuleToHostMsg(NeedReplyMessage msg);

    default void addVmFencerRuleToHost(List<String> vmUuids, String hostUuid) {
        sendVmFencerRuleToHostMsg(generateAddVmFencerRuleToHostMsg(vmUuids, hostUuid));
    }

    default void removeVmFencerRuleFromHost(VmInstanceInventory inv, String hostUuid) {
        sendVmFencerRuleToHostMsg(generateVmFencerRuleFromHostMsg(inv, hostUuid));
    }
}

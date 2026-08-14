package org.zstack.compute.emulatorpinning;

import org.zstack.compute.cpuPinning.CpuPinningConstant;
import org.zstack.compute.vm.MevocoVmSystemTags;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;

import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.i18m;

public class EmulatorPinningFilterFlow implements HostAllocatorFilterExtensionPoint {

    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        String vmUuid = spec.getVmInstance().getUuid();
        if (!MevocoVmSystemTags.VM_EMULATOR_PINNING.hasTag(vmUuid)) {
            return;
        }
        String emulatorPinning = MevocoVmSystemTags.VM_EMULATOR_PINNING.getTokenByResourceUuid(vmUuid, MevocoVmSystemTags.VM_EMULATOR_PINNING_TOKEN);
        if (emulatorPinning.equals("")) {
            return;
        }
        int pCpuMaxId = findPCpuMaxId(emulatorPinning);
        for (HostCandidate candidate : candidates) {
            if (candidate.host.getCapacity().getCpuNum() <= pCpuMaxId) {
                candidate.markAsRejected(getClass(), i18m("vcpu pinning pcpu id > host cores"));
            }
        }
    }

    private int findPCpuMaxId(String emulatorPinning) {
        List<String> emulatorPinningList = Arrays.asList(emulatorPinning.split(CpuPinningConstant.CPU_SET_SEPARATOR));
        return Integer.parseInt(emulatorPinningList.get(emulatorPinningList.size() - 1));
    }
}
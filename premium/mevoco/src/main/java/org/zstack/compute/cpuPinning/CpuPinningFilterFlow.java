package org.zstack.compute.cpuPinning;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.MevocoVmSystemTags;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.i18m;

public class CpuPinningFilterFlow implements HostAllocatorFilterExtensionPoint {
    @Autowired
    CpuPinningFactory cpuPinningFactory;

    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        String vmUuid = spec.getVmInstance().getUuid();
        if (!MevocoVmSystemTags.VM_CPU_PINNING.hasTag(vmUuid)) {
            return;
        }
        String vmCpuPinning = MevocoVmSystemTags.VM_CPU_PINNING.getTokenByResourceUuid(vmUuid, MevocoVmSystemTags.VM_CPU_PINNING_TOKEN);
        List<CpuPinningFactory.CpuPinningRelation> rules = cpuPinningFactory.getRelationFromString(vmCpuPinning);
        rules.removeIf(it -> it.vCpu >= spec.getVmInstance().getCpuNum());
        if (rules.isEmpty()) {
            return;
        }
        int pCpuMaxId = findPCpuMaxId(rules);
        for (HostCandidate candidate : candidates) {
            if (candidate.host.getCapacity().getCpuNum() <= pCpuMaxId) {
                candidate.markAsRejected(getClass(), i18m("vcpu pinning pcpu id > host cores"));
            }
        }
    }

    private int findPCpuMaxId(List<CpuPinningFactory.CpuPinningRelation> rules){
        Set<Long> pCpuSet = new HashSet<>();
        rules.forEach(it -> pCpuSet.addAll(it.pCpuSet));
        return pCpuSet.stream().max(Long::compareTo).orElse(0L).intValue();
    }
}
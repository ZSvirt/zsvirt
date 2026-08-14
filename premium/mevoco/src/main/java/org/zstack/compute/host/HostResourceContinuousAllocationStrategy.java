package org.zstack.compute.host;

import org.zstack.header.host.HostNUMANode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HostResourceContinuousAllocationStrategy extends HostResourceAllocationStrategy implements HostResourceAllocationStrategyInterface {
    public HostResourceContinuousAllocationStrategy() {
        super();
    }

    public HostResourceContinuousAllocationStrategy(String hostUuid, String scene, Map<String, HostNUMANode> numaNodes, List<String> allocatedCpus) {
        super(hostUuid, scene, numaNodes, allocatedCpus);
    }

    @Override
    public void allocate(int vCpuNum, long memSize, boolean cyclicDistribution) {
        if (vCpuNum <= 0) {
            return ;
        }
        if ( !cyclicDistribution && !isCpuNumEnough(vCpuNum)) {
            return ;
        }

        int index = this.availableNodeNum - 1;
        while (true) {
            if (index < 0) {
                // normal scene need cyclic allocate
                if (cyclicDistribution) {
                    index = this.availableNodeNum - 1;
                    clearAllocatedCPUs();
                } else {
                    // vCpuNum > 0 means not enough resource in allocate process, need rollback allocate
                    if (vCpuNum > 0) {
                        failedAllocateAndRollback();
                    }
                    break;
                }
            }

            HostNUMANode node = this.numaNodes.get(String.valueOf(index));

            List<String> nodeAvailableCpus = new ArrayList<>(node.getCpus());
            if (nodeAvailableCpus.size()  < 1) {
                index -= 1;
                continue;
            }

            nodeAvailableCpus.removeAll(this.allocatedCpus);
            if (nodeAvailableCpus.isEmpty()) {
                index -= 1;
                continue;
            }

            if (vCpuNum <= nodeAvailableCpus.size()) {
                List<String> nodeAllocatedCpus = new ArrayList<>(nodeAvailableCpus.subList(0, vCpuNum));

                List<String> failedAllocatedCpus = isPersistentAllocatedCpusToDB(nodeAllocatedCpus);
                nodeAllocatedCpus.removeAll(failedAllocatedCpus);

                this.needAllocatedCpus.addAll(nodeAllocatedCpus);
                vCpuNum -= nodeAllocatedCpus.size();
                index -= 1;

                if (vCpuNum > 0) {
                    continue;
                } else {
                    break;
                }
            }

            List<String> failedAllocatedCpus = isPersistentAllocatedCpusToDB(nodeAvailableCpus);
            nodeAvailableCpus.removeAll(failedAllocatedCpus);

            this.needAllocatedCpus.addAll(nodeAvailableCpus);
            vCpuNum -= nodeAvailableCpus.size();
            index -= 1;
        }
    }
}

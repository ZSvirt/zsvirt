package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.host.HostVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18m;
import static org.zstack.utils.CollectionUtils.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostCapacityAllocatorFlow extends AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(HostCapacityAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private HostCapacityReserveManager reserveMgr;
    @Autowired
    private HostCapacityOverProvisioningManager ratioMgr;
    @Autowired
    private HostCpuOverProvisioningManager cpuRatioMgr;

    private boolean memoryCheck(long vmMemSize, long oldMemory, HostVO hvo) {
        if (HostAllocatorGlobalConfig.HOST_ALLOCATOR_MAX_MEMORY.value(Boolean.class)) {
            if ((vmMemSize + oldMemory) >= hvo.getCapacity().getTotalPhysicalMemory()) {
                return false;
            }
        }

        return ratioMgr.calculateHostAvailableMemoryByRatio(hvo.getUuid(), hvo.getCapacity().getAvailableMemory()) >= vmMemSize;
    }

    private List<HostCandidate> allocate(long cpu, long memory, long oldMemory) {
        return candidates.parallelStream()
                .filter(candidate -> cpu == 0 || enoughCpu(candidate, cpu))
                .filter(candidate -> memory == 0 || enoughMemory(candidate, memory, oldMemory))
                .collect(Collectors.toList());
    }

    private boolean enoughCpu(HostCandidate candidate, long cpu) {
        boolean result = candidate.host.getCapacity().getAvailableCpu() >= cpu;
        if (!result) {
            reject(candidate, i18m("no enough CPU"));
        }
        return result;
    }

    private boolean enoughMemory(HostCandidate candidate, long memory, long oldMemory) {
        boolean result = memoryCheck(memory, oldMemory, candidate.host);
        if (!result) {
            reject(candidate, i18m("no enough memory"));
        }
        return result;
    }

    @Override
    public void allocate() {
        List<HostCandidate> ret = allocate(spec.getCpuCapacity(), spec.getMemoryCapacity(), spec.getOldMemoryCapacity());
        List<HostVO> hosts = reserveMgr.filterOutHostsByReservedCapacity(
                transform(ret, candidate -> candidate.host),
                spec.getCpuCapacity(),
                spec.getMemoryCapacity());
        Set<String> hostUuidSet = transformToSet(hosts, HostVO::getUuid);

        for (HostCandidate candidate : ret) {
            if (!hostUuidSet.contains(candidate.host.getUuid())) {
                reject(candidate, i18m("not enough capacity"));
            }
        }

        next();
    }
}

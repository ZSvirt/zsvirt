package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.i18m;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostStateAndHypervisorAllocatorFlow extends AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(HostStateAndHypervisorAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;

    private void allocate(String hypervisorType) {
        for (HostCandidate candidate : candidates) {
            if (hypervisorType != null && !hypervisorType.equals(candidate.host.getHypervisorType())) {
                reject(candidate, i18m("%s hypervisorType required", hypervisorType));
                continue;
            }

            if (candidate.host.getState() != HostState.Enabled) {
                reject(candidate, i18m("host is not enabled"));
                continue;
            }

            if (candidate.host.getStatus() != HostStatus.Connected) {
                reject(candidate, i18m("host is not connected"));
            }
        }
    }

    @Override
    public void allocate() {
        allocate(spec.getHypervisorType());
        next();
    }
}

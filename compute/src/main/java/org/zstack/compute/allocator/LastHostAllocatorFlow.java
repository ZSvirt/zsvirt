package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCandidate;

import static org.zstack.utils.CollectionUtils.findOneOrNull;

/**
 * Created by mingjian.deng on 2017/11/8.
 *
 * Try to choose a host that the VM starts before
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LastHostAllocatorFlow extends AbstractHostAllocatorFlow {
    @Override
    public void allocate() {
        if (spec.isListAllHosts()) {
            next();
            return;
        }

        String lastHostUuid = spec.getVmInstance().getLastHostUuid();
        if (lastHostUuid == null) {
            next();
            return;
        }

        HostCandidate candidate = findOneOrNull(candidates, arg -> arg.getUuid().equals(lastHostUuid));
        if (candidate != null) {
            recommend(candidate);
        }

        next();
    }
}

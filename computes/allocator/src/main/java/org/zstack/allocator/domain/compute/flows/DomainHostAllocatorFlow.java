package org.zstack.allocator.domain.compute.flows;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DomainHostAllocatorFlow extends AbstractHostAllocatorFlow {
    @Override
    public void allocate() {

    }
}

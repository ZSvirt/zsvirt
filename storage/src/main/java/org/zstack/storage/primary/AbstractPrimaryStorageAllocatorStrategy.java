package org.zstack.storage.primary;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.primary.*;
import org.zstack.storage.primary.allocate.PrimaryStorageAllocatorChainBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.zstack.header.Constants.THREAD_CONTEXT_API;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractPrimaryStorageAllocatorStrategy implements PrimaryStorageAllocatorStrategy {
    private final PrimaryStorageAllocatorChainBuilder allocateBuilder;
    private FlowChainBuilder sortBuilder;
    @Autowired
    private DiskCapacityTracer tracker;

    AbstractPrimaryStorageAllocatorStrategy(PrimaryStorageAllocatorChainBuilder allocateBuilder, FlowChainBuilder sortBuilder) {
        this.allocateBuilder = allocateBuilder;
        this.sortBuilder = sortBuilder;
    }

    @Override
    public PrimaryStorageInventory allocate(PrimaryStorageAllocationSpec spec) {
        return allocateAllCandidates(spec).get(0);
    }

    @Override
    public List<PrimaryStorageInventory> allocateAllCandidates(PrimaryStorageAllocationSpec spec) {
        List<PrimaryStorageVO> candidates;
        candidates = allocateAll(spec);
        Collections.shuffle(candidates);
        candidates = sortAll(spec, candidates);
        return PrimaryStorageInventory.valueOf(candidates);
    }

    private List<PrimaryStorageVO> allocateAll(PrimaryStorageAllocationSpec spec) {
        return allocateBuilder.build().allocate(spec);
    }

    @Override
    public void sort(PrimaryStorageAllocationSpec spec, List<PrimaryStorageVO> candidates) {
        List<PrimaryStorageVO> results = sortAll(spec, candidates);
        candidates.clear();
        candidates.addAll(results);
    }

    private List<PrimaryStorageVO> sortAll(PrimaryStorageAllocationSpec spec, List<PrimaryStorageVO> allocated) {
        List<PrimaryStorageVO> results = new ArrayList<>();
        FlowChain sorterChain = sortBuilder.build();
        String apiId = ThreadContext.get(THREAD_CONTEXT_API);
        sorterChain.setName(String.format("sort-allocated-primary-storage-msg-%s", spec.getAllocationMessage() == null ? apiId : spec.getAllocationMessage().getId()));
        sorterChain.putData(PrimaryStorageConstant.AllocatorParams.SPEC, spec);
        sorterChain.putData(PrimaryStorageConstant.AllocatorParams.CANDIDATES, allocated);
        sorterChain.done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
                results.addAll((List<PrimaryStorageVO>) data.get(PrimaryStorageConstant.AllocatorParams.CANDIDATES));
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                results.addAll(allocated);
            }
        });

        tracker.trackAllocatorChain(sorterChain);
        sorterChain.start();
        return results;
    }
}

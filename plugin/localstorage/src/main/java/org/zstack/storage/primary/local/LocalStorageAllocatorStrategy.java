package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageAllocatorStrategy;
import org.zstack.header.storage.primary.PrimaryStorageConstant.AllocatorParams;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.storage.primary.DiskCapacityTracer;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by frank on 7/1/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageAllocatorStrategy implements PrimaryStorageAllocatorStrategy {
    private FlowChainBuilder builder;

    @Autowired
    private DiskCapacityTracer tracker;

    public LocalStorageAllocatorStrategy(FlowChainBuilder builder) {
        this.builder = builder;
    }

    @Override
    public PrimaryStorageInventory allocate(PrimaryStorageAllocationSpec spec) {
        return allocateAllCandidates(spec).get(0);
    }

    @Override
    public void sort(PrimaryStorageAllocationSpec spec, List<PrimaryStorageVO> candidates) {
    }

    @Override
    public List<PrimaryStorageInventory> allocateAllCandidates(PrimaryStorageAllocationSpec spec) {
        class Result {
            List<PrimaryStorageVO> result;
            ErrorCode errorCode;
        }

        final Result ret = new Result();
        FlowChain allocatorChain = builder.build();
        allocatorChain.setName(String.format("allocate-local-primary-storage-msg-%s", spec.getAllocationMessage().getId()));
        allocatorChain.setData(map(e(AllocatorParams.SPEC, spec)));
        allocatorChain.done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
                ret.result = (List<PrimaryStorageVO>) data.get(AllocatorParams.CANDIDATES);
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                ret.errorCode = errCode;
            }
        });

        tracker.trackAllocatorChain(allocatorChain);

        allocatorChain.start();

        if (ret.errorCode != null) {
            throw new OperationFailureException(ret.errorCode);
        } else {
            List<PrimaryStorageInventory> invs = PrimaryStorageInventory.valueOf(ret.result);
            Collections.shuffle(invs);
            return invs;
        }
    }
}

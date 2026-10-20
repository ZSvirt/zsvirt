package org.zstack.storage.primary.allocate;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageErrors;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageAllocatorTrigger;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;
import org.zstack.storage.primary.DiskCapacityTracer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.i18n;
import static org.zstack.utils.CollectionUtils.transform;

/**
 * Synchronous primary storage filter chain.
 * Producers and filters are both {@link AbstractPrimaryStorageAllocatorFlow}.
 * A flow rejects candidates; this chain reports an error only when none remain,
 * or when a flow calls {@link AbstractPrimaryStorageAllocatorFlow#fail}.
 */
public class PrimaryStorageAllocatorChain implements PrimaryStorageAllocatorTrigger {
    private static final CLogger logger = Utils.getLogger(PrimaryStorageAllocatorChain.class);

    private PrimaryStorageAllocationSpec allocationSpec;
    private List<AbstractPrimaryStorageAllocatorFlow> flows;
    private Iterator<AbstractPrimaryStorageAllocatorFlow> it;
    private AbstractPrimaryStorageAllocatorFlow lastFlow;

    private final List<PrimaryStorageCandidate> result = new ArrayList<>();
    private final List<PrimaryStorageCandidate.RejectedCandidate> rejectedList = new ArrayList<>();
    private ErrorCode errorCode;

    public void setFlows(List<AbstractPrimaryStorageAllocatorFlow> flows) {
        this.flows = flows;
    }

    public List<PrimaryStorageVO> allocate(PrimaryStorageAllocationSpec spec) {
        this.allocationSpec = spec;
        this.it = flows.iterator();
        if (!it.hasNext()) {
            throw err(PrimaryStorageErrors.ALLOCATE_ERROR, "primary storage allocation flow chain is empty")
                    .toException();
        }

        runFlow(it.next());
        if (errorCode != null) {
            throw errorCode.toException();
        }
        return transform(result, candidate -> candidate.primaryStorage);
    }

    private void runFlow(AbstractPrimaryStorageAllocatorFlow flow) {
        try {
            lastFlow = flow;
            flow.setCandidates(result);
            flow.setSpec(allocationSpec);
            flow.setTrigger(this);
            flow.allocate();
        } catch (OperationFailureException ofe) {
            if (errorCode == null) {
                errorCode = ofe.getErrorCode();
                if (!rejectedList.isEmpty()) {
                    fillRejectI18n();
                    errorCode.withOpaque("rejectedCandidates", rejectedList);
                }
            }
        } catch (Throwable t) {
            logger.warn("unhandled throwable", t);
            errorCode = err(PrimaryStorageErrors.ALLOCATE_ERROR, t.getMessage());
        }
    }

    @Override
    public void next() {
        boolean anyAllowed = false;
        for (Iterator<PrimaryStorageCandidate> iterator = result.iterator(); iterator.hasNext(); ) {
            PrimaryStorageCandidate candidate = iterator.next();
            if (candidate.reject != null) {
                logger.debug(String.format(
                        "[Primary Storage Allocation]: flow[%s] rejected candidate primary storage[uuid:%s, name:%s]: %s",
                        candidate.rejectBy, candidate.getUuid(), candidate.primaryStorage.getName(), candidate.reject));
                iterator.remove();
                rejectedList.add(candidate.toRejectedCandidate());
                continue;
            }
            anyAllowed = true;
        }

        if (!anyAllowed) {
            errorCode = noCandidateError();
            throw errorCode.toException();
        }

        logger.debug(String.format("[Primary Storage Allocation]: flow[%s] left %s candidates",
                lastFlow.getClass().getName(), result.size()));
        DiskCapacityTracer.traceAllocation(allocationSpec, transform(result, candidate -> candidate.primaryStorage));

        if (it.hasNext()) {
            runFlow(it.next());
        }
    }

    @Override
    public void fail(ErrorCode errorCode) {
        throw errorCode.toException();
    }

    private ErrorCode noCandidateError() {
        fillRejectI18n();
        ErrorCode error;
        if (rejectedList.size() == 1) {
            PrimaryStorageCandidate.RejectedCandidate candidate = rejectedList.get(0);
            error = err(PrimaryStorageErrors.ALLOCATE_ERROR,
                    "failed to allocate primary storage[%s]: %s", candidate.primaryStorageUuid, candidate);
        } else {
            error = err(PrimaryStorageErrors.ALLOCATE_ERROR,
                    "[Primary Storage Allocation] no primary storage meet the requirements");
        }

        error.withOpaque("rejectedCandidates", rejectedList);
        return error;
    }

    private void fillRejectI18n() {
        for (PrimaryStorageCandidate.RejectedCandidate candidate : rejectedList) {
            if (candidate.rejectI18n == null) {
                candidate.rejectI18n = i18n(candidate.reject);
            }
        }
    }
}
